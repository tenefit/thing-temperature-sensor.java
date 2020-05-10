/**
 * Copyright 2016-2020 Tenefit. All rights reserved.
 */
package com.tenefit.demo.things.temperatureSensor;

import java.net.URI;
import java.net.URISyntaxException;
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
import com.github.rvesse.airline.annotations.restrictions.Pattern;
import com.github.rvesse.airline.annotations.restrictions.Required;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.hivemq.client.mqtt.mqtt5.Mqtt5ClientBuilder;
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
        description = "Scheme, address, and optional port for broker. Scheme values: mqtt|mqtts. Port default: 1883|8883" +
            "Example: mqtts://mqtt.example.com")
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
    private String sensorsTopicArg;

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

    @Option(
        name = { "--state" },
        description = "Initial state for this sensor. Values: [ON|OFF] default is ON")
    @Pattern(pattern = "ON|OFF", flags = java.util.regex.Pattern.CASE_INSENSITIVE)
    @Once
    @NotBlank
    @NotEmpty
    private String stateOption;

    private String sensorsTopic;

    private SensorState state;

    private Mqtt5BlockingClient client;

    public static void main(String[] args) throws InterruptedException, ExecutionException, URISyntaxException
    {
        SingleCommand<TemperatureSensor> parser = SingleCommand.singleCommand(TemperatureSensor.class);
        TemperatureSensor temperatureSensor = parser.parse(args);
        temperatureSensor.start();
    }

    public void start() throws InterruptedException, URISyntaxException
    {
        if (help.showHelpIfRequested())
        {
            return;
        }

        if (stateOption != null)
        {
            state = SensorState.valueOf(stateOption.toUpperCase());
        }
        else
        {
            state = SensorState.ON;
        }

        sensorsTopic = String.format("%s/%s", sensorsTopicArg, id);

        final URI uri = new URI(brokerAddress);
        client = mqtt5ClientBuilder(Mqtt5Client.builder(), uri.getScheme())
            .serverHost(uri.getHost())
            .identifier(UUID.randomUUID().toString())
            .automaticReconnectWithDefaultConfig()
            .addConnectedListener(c -> System.out.format("sensor %s connected, row %s, state %s\n", id, row, state))
            .addDisconnectedListener(c -> System.out.format("sensor %s disconnected, auto-reconnecting...\n", id))
            .buildBlocking();

        client.connect();

        ControlSubscriber controlSubscriber = new ControlSubscriber(
            client,
            String.format("%s/%s", stateTopic, id),
            String.format("%s/%s", controlTopic, id),
            id,
            row,
            state,
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

    private <T extends Mqtt5ClientBuilder>  T mqtt5ClientBuilder(T builder, String scheme)
    {
        if ("mqtts".equals(scheme))
        {
            builder.sslWithDefaultConfig();
        }
        return builder;
    }

    private void publishReadingIfNecessary()
    {
        if (state == SensorState.ON)
        {
            int temp = ThreadLocalRandom.current().nextInt(minTemp, maxTemp + 1);
            String readingMessage = String.format("{\"id\":\"%s\",\"unit\":\"%s\",\"value\":%d,\"row\":\"%s\"}",
                id, temperatureUnit.value(), temp, row);
            Mqtt5UserProperties userProperties = Mqtt5UserProperties.builder()
                .add("row", row)
                .build();
            client.publishWith()
                .topic(sensorsTopic)
                .payload(readingMessage.getBytes())
                .userProperties(userProperties)
                .qos(MqttQos.AT_MOST_ONCE)
                .send();
        }
    }

    private void handleStateChange(
        SensorState newState)
    {
        if (state != newState)
        {
            state = newState;
            // Publish immediately to appear responsive rather than waiting until
            // the next message comes along
            publishReadingIfNecessary();
        }
    }
}
