/**
 * Copyright 2016-2020 Tenefit. All rights reserved.
 */
package com.tenefit.demo.things.temperatureSensor;

import java.util.function.Consumer;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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

    final Gson gson;

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
        this.gson = new Gson();
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
                Mqtt5Publish result = publishes.receive();
                JsonObject message = gson.fromJson(new String(result.getPayloadAsBytes()), JsonObject.class);
                JsonElement stateEl = message.get("state");
                if (stateEl == null)
                {
                    return;
                }
                try
                {
                    SensorState newState = SensorState.valueOf(stateEl.getAsString());
                    stateChangeHandler.accept(newState);
                    String newStateMessage = String.format("{\"id\":\"%s\",\"row\":\"%s\",\"state\":\"%s\"}",
                        id, row, newState);
                    client.publishWith()
                        .topic(String.format("%s/%s", stateTopic, id))
                        .payload(newStateMessage.getBytes())
                        .qos(MqttQos.AT_MOST_ONCE)
                        .send();
                }
                catch (IllegalArgumentException ex)
                {
                    // do nothing
                }

            }
        }
        catch (InterruptedException ex)
        {
            throw new RuntimeException(ex);
        }
    }
}
