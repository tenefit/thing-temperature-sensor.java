# Sensors end-to-end demo

This README contains instructions for running the full sensors demo end-to-end.

## Contents

- [Prerequisites](#prerequisites)
- [One-time setup](#one-time-setup)
- [Pre-demo setup](#pre-demo-setup)
- [Running the demo](#running-the-demo)
- [Post-demo cleanup](#post-demo-cleanup)

## Prerequisites

- Docker and Docker Compose

## One-time setup

The steps in this section only need to be done once to setup and install the necessary components on your machine.

1. Clone or fork this repo.

1. You need to build the MQTT sensor IoT thing. Open a new terminal window and to the top level directory of this repo. You'll know you're in the right directory if you see the `pom.xml` file.

1. Run the following command to build the Java-based MQTT sensor application using Maven:

   ```
   $ ./mvnw clean install
   ```

   The very first time you ever run that command, it may take a minute to pull down library dependencies and cache them locally. Thereafter, whenever you run that command it should only take a few seconds.

## Pre-demo setup

Go through these prior to each occasion you wan to run the demo.

1. Open a terminal window and go to the top-level directory from this repo. To check that you're in the right directory, make sure you see the `docker-compose.yml` file:

   ```
   $ ls docker-compose.yml
   docker-compose.yml
   ```

1. Start the Docker Compose suite that will run the Eclipse Mosquitto MQTT broker and Apache Kafka Connect:

   ```
   $ docker-compose up -d
   ```

   The `-d` argument will run the Docker Compose suite in the background (daemon mode). Later when you want to shut it down, you can run:

   To see the Docker Compose logs while it's starting, run:

   ```
   $ docker-compose logs --follow
   ```

   You'll need to wait between one and two minutes for everything to start. It's ready when you generally see the logging stop scrolling and say `INFO Finished starting connectors and tasks`, for example:

   ```
   kafka-connect    | [2020-04-23 17:40:27,971] INFO Finished starting connectors and tasks (org.apache.kafka.connect.runtime.distributed.DistributedHerder)
   ```

   Note that some logging may continue after that line, so it may not necessarily be on the very last line.

1. Once it's built, start the MQTT sensors:

   ```
   $ scripts/runall.sh mqtt://localhost
   ```

   That will start 10 sensors in the background. A moment later you should see:

   ```
   sensor 1 connected
   sensor 2 connected
   sensor 3 connected
   ...
   ```

1. Open the sensor portal in a browser: https://demo.tenefit.cloud/#sensors

1. You should see the sensors updating.

1. Use the lower-right corner to change the temperature unit and verify it works.

1. When everything is working, you're ready to run the demo.

## Running the demo

1. Open the sensor portal in a browser: https://demo.tenefit.cloud/#sensors

1. Show sensors updating, changing the temperature unit, etc. Note that you will not be able to change the state of the sensor.

1. Kill the sensors:

   ```
   $ scripts/killall.sh
   ```

1. Now restart the sensors, but this time going through tenefit.cloud:

   ```
   $ scripts/runall.sh mqtt+tls://mqtt.demo.tenefit.cloud
   ```

   That will start 10 sensors in the background. A moment later you should see:

   ```
   sensor 1 connected
   sensor 2 connected
   sensor 3 connected
   ...
   ```

1. Return back to the sensor portal in a browser: https://demo.tenefit.cloud/#sensors

1. Show the flow once again. This time you will be update the state.

## Post-demo cleanup

1. Stop the sensors:

   ```
   $ scripts/killall.sh
   ```

1. Stop the Docker Compose suite:

   ```
   $ docker-compose down
   ```

1. Quit Docker, if you no longer need it.
