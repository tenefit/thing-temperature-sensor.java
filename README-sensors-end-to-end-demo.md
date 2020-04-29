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

- Java 1.8 or higher

## One-time setup

The steps in this section only need to be done once to setup and install the necessary components on your machine.

1. Clone or fork this repo.

1. You need to build the MQTT sensor IoT thing. In a terminal window, change to the top level directory of this repo. You'll know you're in the right directory if you see the `pom.xml` file:

   ```
   $ ls pom.xml
   pom.xml
   ```

1. Run the following command to build the Java-based MQTT sensor application using Maven:

   ```
   $ ./mvnw clean install
   ```

   The very first time you ever run that command, it may take a minute to pull down library dependencies and cache them locally. Thereafter, whenever you run that command it should only take a few seconds.

## Pre-demo setup

Go through these steps prior to each occasion you want to run the demo.

1. Open a terminal window and go to the top-level directory from this repo. To check that you're in the right directory, make sure you see the `docker-compose.yml` file:

   ```
   $ ls docker-compose.yml
   docker-compose.yml
   ```

1. Start the Docker Compose suite that will run the Eclipse Mosquitto MQTT broker and Apache Kafka Connect:

   ```
   $ docker-compose up
   ```

   You'll need to wait between one and two minutes for everything to start. It's ready when you generally see the logging stop scrolling.

   Often, the following lines indicate that the Docker containers are ready:

   ```
   mosquitto | 1588138801: New connection from 10.8.8.3 on port 1883.
   mosquitto | 1588138801: New connection from 10.8.8.3 on port 1883.
   mosquitto | 1588138801: New client connected from 10.8.8.3 as paho7385846549894 (p2, c1, k60).
   mosquitto | 1588138801: New client connected from 10.8.8.3 as paho7385846530194 (p2, c1, k60).
   ```

## Running the demo

1. Start the MQTT sensors:

   ```
   $ scripts/runall.sh mqtt://localhost
   ```

   That will start 10 sensors in the background, connected to the Mosquitto MQTT broker running in Docker. A moment later you should see:

   ```
   sensor 1 connected
   sensor 2 connected
   sensor 3 connected
   ...
   ```

1. Open the sensor portal in a browser: https://demo.tenefit.cloud/#sensors

1. You should see the sensors updating.

   Use the lower-right corner to change the temperature unit and verify it works.

   Note that you cannot update the state yet, when using the sensors running against the Docker container.

1. Kill the sensors:

   ```
   $ scripts/killall.sh
   ```

1. Now restart the sensors, but this time connecting to tenefit.cloud:

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

1. Stop the Docker Compose suite by pressing `Ctrl-C`.

1. Shutdown Docker itself, if you no longer need it.
