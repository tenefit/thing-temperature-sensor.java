/**
 * Copyright 2016-2020 Tenefit. All rights reserved.
 */
package com.tenefit.demo.things.temperatureSensor;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;

import javax.inject.Inject;

import com.github.rvesse.airline.HelpOption;
import com.github.rvesse.airline.SingleCommand;
import com.github.rvesse.airline.annotations.Command;
import com.github.rvesse.airline.annotations.Option;
import com.github.rvesse.airline.annotations.restrictions.NotBlank;
import com.github.rvesse.airline.annotations.restrictions.NotEmpty;
import com.github.rvesse.airline.annotations.restrictions.Once;
import com.github.rvesse.airline.annotations.restrictions.Required;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.hivemq.client.mqtt.mqtt5.datatypes.Mqtt5UserProperties;

@Command(name = "temperature-sensor", description = "Thing for publishing temperature readings")
public class TemperatureSensor
{
    public enum SensorState
    {
        ON, OFF
    }

    public enum TemperatureUnit
    {
        CELSIUS("C"),
        FAHRENHEIT("F"),
        KELVIN("K");

        private final String value;

        TemperatureUnit(
            String value)
        {
            this.value = value;
        }

        public String value()
        {
            return value;
        }
    }

    private final TemperatureUnit temperatureUnit = TemperatureUnit.CELSIUS;

    private final int minTemp = 75;
    private final int maxTemp = 200;

    // milliseconds
    private final long minReadingDelay = 500;
    private final long maxReadingDelay = 2000;

    @Inject
    protected HelpOption<TemperatureSensor> help;

    @Option(
        name = { "--broker", "-b" },
        description = "Address and optional port for broker. Port defaults to 1883.")
    @Required
    @Once
    @NotBlank
    @NotEmpty
    private String brokerAddress;

    @Option(
        name = { "--state-topic" },
        description = "Output topic for state updates")
    @Required
    @Once
    @NotBlank
    @NotEmpty
    private String stateTopic;

    @Option(
        name = { "--sensors-topic" },
        description = "Output topic for sensor readings")
    @Required
    @Once
    @NotBlank
    @NotEmpty
    private String sensorsTopic;

    @Option(
        name = { "--control-topic" },
        description = "Input topic for control messages")
    @Required
    @Once
    @NotBlank
    @NotEmpty
    private String controlTopic;

    @Option(
        name = { "--id" },
        description = "Id for this sensor")
    @Required
    @Once
    @NotBlank
    @NotEmpty
    private String id;

    @Option(
        name = { "--row" },
        description = "Row this sensor is in")
    @Required
    @Once
    @NotBlank
    @NotEmpty
    private String row;

    private SensorState sensorState;

    private Mqtt5BlockingClient client;

    public static void main(String[] args) throws InterruptedException, ExecutionException
    {
        SingleCommand<TemperatureSensor> parser = SingleCommand.singleCommand(TemperatureSensor.class);
        TemperatureSensor temperatureSensor = parser.parse(args);
        temperatureSensor.start();
    }

    public TemperatureSensor()
    {
        sensorState = SensorState.ON;
    }

    public void start() throws InterruptedException
    {
        if (help.showHelpIfRequested())
        {
            return;
        }

        client = Mqtt5Client.builder()
            .identifier(UUID.randomUUID().toString())
            .serverHost(brokerAddress)
            .buildBlocking();

        client.connect();

        System.out.format("sensor %s connected\n", id);

        ControlSubscriber controlSubscriber = new ControlSubscriber(
            client,
            stateTopic,
            controlTopic,
            id,
            row,
            this::handleStateChange);
        Thread stateSubscriberThread = new Thread(controlSubscriber, "stateSubscriber-thread");
        stateSubscriberThread.start();

        while (true)
        {
            publishReadingIfNecessary();
            long delay = ThreadLocalRandom.current().nextLong(minReadingDelay, maxReadingDelay + 1);
            Thread.sleep(delay);
        }
    }

    private void publishReadingIfNecessary()
    {
        if (sensorState == SensorState.ON)
        {
            int temp = ThreadLocalRandom.current().nextInt(minTemp, maxTemp + 1);
            String readingMessage = String.format("{\"id\":\"%s\",\"unit\":\"%s\",\"value\":\"%d\"}",
                id, temperatureUnit, temp);
            Mqtt5UserProperties userProperties = Mqtt5UserProperties.builder()
                .add("row", row)
                .build();
            client.publishWith()
                .topic(String.format("%s/%s", sensorsTopic, id))
                .payload(readingMessage.getBytes())
                .userProperties(userProperties)
                .qos(MqttQos.AT_MOST_ONCE)
                .send();
        }
    }

    private void handleStateChange(
        SensorState state)
    {
        if (state != sensorState)
        {
            sensorState = state;
            // Publish immediately to appear responsive rather than waiting until
            // the next message comes along
            publishReadingIfNecessary();
        }
    }
}
