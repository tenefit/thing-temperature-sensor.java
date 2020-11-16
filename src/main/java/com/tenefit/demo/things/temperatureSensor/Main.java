/**
 * Copyright 2016-2020 Tenefit. All rights reserved.
 */
package com.tenefit.demo.things.temperatureSensor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import com.github.rvesse.airline.HelpOption;
import com.github.rvesse.airline.SingleCommand;
import com.github.rvesse.airline.annotations.Command;
import com.github.rvesse.airline.annotations.Option;
import com.github.rvesse.airline.annotations.restrictions.NotBlank;
import com.github.rvesse.airline.annotations.restrictions.NotEmpty;
import com.github.rvesse.airline.annotations.restrictions.Once;
import com.github.rvesse.airline.annotations.restrictions.Required;
import com.github.rvesse.airline.annotations.restrictions.ranges.IntegerRange;
import com.github.rvesse.airline.annotations.restrictions.ranges.LongRange;

@Command(name = "temperature-sensor", description = "Thing for publishing temperature readings")
public class Main
{
    @Inject
    protected HelpOption<Main> help;

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
        name = { "--sensors" },
        description = "The number of sensors to start. Defaults to 1")
    @Once
    @NotBlank
    @NotEmpty
    @IntegerRange(min = 1)
    private int sensors = 1;

    @Option(
        name = { "--rows" },
        description = "The number of rows. Defaults to 1")
    @Once
    @NotBlank
    @NotEmpty
    @IntegerRange(min = 1)
    private int rows = 1;

    @Option(
        name = { "--min-interval" },
        description = "The minimum time before a sensor will send the next reading, in milliseconds. Defaults to 500")
    @Once
    @NotBlank
    @NotEmpty
    @LongRange(min = 1)
    private long minInterval = 500;

    @Option(
        name = { "--max-interval" },
        description = "The maximum time before a sensor will send the next reading, in milliseconds. Defaults to 2000")
    @Once
    @NotBlank
    @NotEmpty
    @LongRange(min = 1)
    private long maxInterval = 2000;

    @Option(
        name = { "--state-topic" },
        description = "Output topic for state updates. Use %i variable for sensor id. Defaults to /state/%i")
    @Once
    @NotBlank
    @NotEmpty
    private String stateTopic = "/state/%i";

    @Option(
        name = { "--sensors-topic" },
        description = "Output topic for sensor readings. Use %i variable for sensor id. Defaults to /sensors/%i")
    @Once
    @NotBlank
    @NotEmpty
    private String sensorsTopic = "/sensors/%i";

    @Option(
        name = { "--control-topic" },
        description = "Input topic for control messages. Use %i variable for sensor id. Defaults to /control/%i")
    @Once
    @NotBlank
    @NotEmpty
    private String controlTopic = "/control/%i";

    @Option(
        name = { "--verbose", "-v" },
        description = "Show verbose output at start up")
    @Once
    private boolean verbose;

    public static void main(String[] args) throws Exception
    {
        SingleCommand<Main> parser = SingleCommand.singleCommand(Main.class);
        Main temperatureSensor = parser.parse(args);
        temperatureSensor.start();
    }

    public void start()
    {
        if (help.showHelpIfRequested())
        {
            return;
        }

        ExecutorService executorService = Executors.newFixedThreadPool(sensors);
        int row = 1;
        for (int i = 0; i < sensors; i++)
        {
            final String id = String.valueOf(i + 1);
            final TemperatureSensor sensor = new TemperatureSensor(
                id,
                String.valueOf(row),
                brokerAddress,
                stateTopic.replaceAll("%i", id),
                sensorsTopic.replaceAll("%i", id),
                controlTopic.replaceAll("%i", id),
                minInterval,
                maxInterval);

            executorService.execute(sensor);

            if ((i + 1) % rows == 0 && row < rows)
            {
                row++;
            }
        }
    }
}
