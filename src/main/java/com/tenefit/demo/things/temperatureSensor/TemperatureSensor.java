/**
 * Copyright 2016-2020 Tenefit. All rights reserved.
 */
package com.tenefit.demo.things.temperatureSensor;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

import com.github.rvesse.airline.annotations.Command;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.hivemq.client.mqtt.mqtt5.Mqtt5ClientBuilder;
import com.hivemq.client.mqtt.mqtt5.datatypes.Mqtt5UserProperties;

@Command(name = "temperature-sensor", description = "Thing for publishing temperature readings")
public class TemperatureSensor implements Runnable
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

    private final String id;
    private final String row;

    private final String brokerAddress;
    private final String stateTopic;
    private final String sensorsTopic;
    private final String controlTopic;
    private final long minInterval;
    private final long maxInterval;
    private final Consumer<Long> incMessages;

    private SensorState state;

    private Mqtt5BlockingClient client;

    public TemperatureSensor(
        String id,
        String row,
        String brokerAddress,
        String stateTopic,
        String sensorsTopic,
        String controlTopic,
        long minInterval,
        long maxInterval,
        Consumer<Long> incMessages)
    {
        this.id = id;
        this.row = row;
        this.brokerAddress = brokerAddress;
        this.stateTopic = stateTopic;
        this.sensorsTopic = sensorsTopic;
        this.controlTopic = controlTopic;
        this.minInterval = minInterval;
        this.maxInterval = maxInterval >= minInterval ? maxInterval : minInterval;
        this.incMessages = incMessages;

        state = SensorState.ON;
    }

    @Override
    public void run()
    {
        URI uri;
        try
        {
            uri = new URI(brokerAddress);
        }
        catch (URISyntaxException e)
        {
            throw new RuntimeException(e);
        }
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
            stateTopic,
            controlTopic,
            id,
            row,
            state,
            this::handleStateChange);
        Thread stateSubscriberThread = new Thread(controlSubscriber, "stateSubscriber-thread");
        stateSubscriberThread.start();

        while (true)
        {
            publishReadingIfNecessary();
            long delay = ThreadLocalRandom.current().nextLong(minInterval, maxInterval + 1);
            try
            {
                Thread.sleep(delay);
            }
            catch (InterruptedException e)
            {
                throw new RuntimeException(e);
            }
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
            incMessages.accept(1L);
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
