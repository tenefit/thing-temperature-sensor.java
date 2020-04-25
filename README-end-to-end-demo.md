# End to end demo

## Pre-demo setup

Follow these steps to prepare for showing the demo.

1. Download [Kafka Connect MQTT](https://www.confluent.io/hub/confluentinc/kafka-connect-mqtt) and unzip it in the top directory of this repo. You should end up with a directory called something like `confluentinc-kafka-connect-mqtt-1.2.3`.

1. Edit `docker-compose.yml`, find the `kafka-connect` service, then the `volumes` section in that service. Change `./confluentinc-kafka-connect-mqtt-1.2.3` in the following line, to match the directory you just unzipped to:

   ```
   - ./confluentinc-kafka-connect-mqtt-1.2.3/lib:/etc/kafka-connect/jars
   ```

1. Build the custom transformer (from a separate repo, [TODO instructions to be added]) and place the jar file into `confluentinc-kafka-connect-mqtt-1.2.3/lib`, or whatever your unzipped directory is called.

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

   ```
   $ docker-compose down
   ```

   from the same directory.

1. To see the Docker Compose logs, run:

   ```
   $ docker-compose logs --follow
   ```

   You'll need to wait a minute or two for everything to start. It's probably ready when you see `INFO Finished starting connectors and tasks`:

   ```
   kafka-connect    | [2020-04-23 17:40:27,971] INFO Finished starting connectors and tasks (org.apache.kafka.connect.runtime.distributed.DistributedHerder)
   ```

1. This step only needs to be done once per Kafka cluster. It has already been done for `kafka.tenefit.cloud`, but is preserved here for future reference.

   Run the following command to configure Kafka Connect:

   ```bash
   $ curl \
     --header "Content-Type: application/json" \
     --header "Accept: application/json" \
     --request PUT \
     --data @connect-mqtt.json http://localhost:8083/connectors/mqtt-source/config
   ```

1. Build the MQTT sensor. Open a new terminal window and to the top level directory of this repo. You'll know you're in the right directory if you see the `pom.xml` file.

1. Run the following command to build the Java-based MQTT sensor application:

   ```
   $ mvn clean install
   ```

   The very first time you ever run that command, it may take a minute to pull down library dependencies. Thereafter, whenever you run that command it should only take a few seconds.

1. Once it's built, start an MQTT sensors:

   ```
   $ scripts/runall.sh
   ```

   That will start sensors in the background. A moment later you should see:

   ```
   sensor 1 connected
   sensor 2 connected
   sensor 3 connected
   ...
   ```

   Later, when you want to stop the sensors, you can run:

   ```
   $ scripts/killall.sh
   ```

1. Open the sensor portal in a browser: https://demo.tenefit.cloud/#sensors

1. You should see the one sensor with temperature updates.

1. Use the lower-right corner to change the temperature unit and verify it works.

1. Go to the single sensor view and turn the state on and off to verify it works. TODO Will this work through Mosquitto in Docker?

1. When everything is working, you're ready to run the demo.

## Post-demo clean up

1. Stop the sensors:

   ```
   $ scripts/killall.sh
   ```

1. Stop the Docker Compose suite:

   ```
   $ docker-compose down
   ```

1. Crack open a beer.

## Notes

Download [Kafka Connect MQTT](https://www.confluent.io/hub/confluentinc/kafka-connect-mqtt)

Publish to mosquitto broker from docker command:

```
$ docker-compose exec \
    mosquitto \
    /usr/bin/mosquitto_pub \
      -h localhost \
      -p 1883 \
      -t sensors/1 \
      -m '{ "id": "1", "unit": "C", "value": 75 }'
```

Rest API: https://docs.confluent.io/current/connect/references/restapi.html

Remove the Kafka Connect configuration:

```bash
$ curl -X DELETE http://localhost:8083/connectors/mqtt-source
```

Publish new configuration:

```bash
$ curl --header "Content-Type: application/json" --request POST  --data '{"name": "mqtt-source", "config": {"connector.class": "io.confluent.connect.mqtt.MqttSourceConnector", "tasks.max": 1, "mqtt.server.uri": "tcp://mosquitto:1883", "mqtt.topics": "sensors/#", "kafka.topic": "sensors", "value.converter": "org.apache.kafka.connect.converters.ByteArrayConverter", "confluent.topic.bootstrap.servers": "kafka.tenefit.cloud:9092", "confluent.topic.replication.factor": 1 }}' http://localhost:8083/connectors
```

Add a new configuration:

```bash
$ curl --header "Content-Type: application/json" --header "Accept: application/json" --request POST --data @connect-mqtt.json http://localhost:8083/connectors
```

Add or update configuration (note the name in the URL):

```bash
$ curl --header "Content-Type: application/json" --header "Accept: application/json" --request PUT --data @connect-mqtt.json http://localhost:8083/connectors/mqtt-source/config
```

Get the connector configuration name:

```bash
$ curl --header "Accept: application/json" http://localhost:8083/connectors
```

Get the connector configuration details for the given configuration name:

```bash
$ curl --header "Accept: application/json" http://localhost:8083/connectors/mqtt-source/config | jq
```

Get the connector configuration state for the given configuration name:

```bash
$ curl --header "Accept: application/json" http://localhost:8083/connectors/mqtt-source/status | jq
```

Restart the connector configuration for the given configuration name:

```bash
$ curl --request POST http://localhost:8083/connectors/mqtt-source/restart
```

Validate plugin:

```bash
$ curl --header "Accept: application/json" http://localhost:8083/connector-plugins/InsertUuid/config/validate | jq
```

NOTES:

"value.converter": "org.apache.kafka.connect.json.JsonConverter",
"transforms": "ValueToKey",
"transforms.ValueToKey.type": "org.apache.kafka.connect.transforms.ValueToKey",
"transforms.ValueToKey.fields": "id",
