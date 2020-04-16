/**
 * Copyright 2016-2020 Tenefit. All rights reserved.
 */
package com.tenefit.demo.things.temperatureSensor;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.StringReader;
import java.util.function.Consumer;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import com.hivemq.client.mqtt.MqttGlobalPublishFilter;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient.Mqtt5Publishes;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import com.tenefit.demo.things.temperatureSensor.TemperatureSensor.SensorState;

/**
 * Subscribe to state messages which modify the state of this sensor.
 */
public class ControlSubscriber implements Runnable
{
    final Mqtt5BlockingClient client;

    final String stateTopic;
    final String controlTopic;
    final String id;
    final String row;
    final Consumer<SensorState> stateChangeHandler;

    public ControlSubscriber(
        Mqtt5BlockingClient client,
        String stateTopic,
        String controlTopic,
        String id,
        String row,
        Consumer<SensorState> stateChangeHandler)
    {
        this.client = client;
        this.stateTopic = stateTopic;
        this.controlTopic = controlTopic;
        this.id = id;
        this.row = row;
        this.stateChangeHandler = stateChangeHandler;
    }

    @Override
    public void run()
    {
        try (Mqtt5Publishes publishes = client.publishes(MqttGlobalPublishFilter.ALL))
        {
            client.subscribeWith()
                .topicFilter(String.format("%s/%s", controlTopic, id))
                .qos(MqttQos.AT_MOST_ONCE)
                .send();
            while (true)
            {
                Mqtt5Publish inputMessage = publishes.receive();
                try (JsonReader inputJson = Json.createReader(new StringReader(new String(inputMessage.getPayloadAsBytes()))))
                {
                    JsonObject input = inputJson.readObject();
                    SensorState state = SensorState.valueOf(input.getString("state"));
                    if (state != null)
                    {
                        stateChangeHandler.accept(state);
                        String topic = String.format("%s/%s", stateTopic, id);
                        byte[] output = Json.createObjectBuilder()
                            .add("id", id)
                            .add("row", row)
                            .add("state", state.toString())
                            .build()
                            .toString()
                            .getBytes(UTF_8);
                        client.publishWith()
                            .topic(topic)
                            .payload(output)
                            .qos(MqttQos.AT_MOST_ONCE)
                            .send();
                    }
                }
            }
        }
        catch (InterruptedException ex)
        {
            throw new RuntimeException(ex);
        }
    }
}
