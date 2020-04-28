# thing-temperature-sensor.java

A single sensor that will publish temperature readings at random intervals.

At start up, the sensor will publish it's state. It will also listen for control messages that modify the sensor's state. After any state change, the sensor will publish it's new state.

In the following sections, the following values are assumed:

- `id` is `4`
- `row` is `9`

### Publishing state

At startup, and anytime the state is changed, the sensor will publish the state.

- **topic:** `/state/4` (specified with `--state-topic`)
- **headers:** none
- **payload:**
  ```json
  { "id": "4", "row": "9", "state": "ON" }
  ```

### Receiving control messages

At startup, the sensor subscribes for control messages. When a message is received, the sensor's state is updated. If it is set to `OFF`, it will stop publishing readings. If it is set to `ON`, it will resume publishing readings.

In addition, the sensor will publish its new state (see [Publishing state](#publishing-state) above).

- **topic:** `/control/4` (specified with `--control-topic`)
- **headers:** none
- **payload:**
  ```json
  { "state": "OFF" }
  ```

### Publishing readings

If the sensor state is `ON`, it will publish readings at random interals. All temperatures are published in Celsius.

- **topic:** `/sensors/4` (specified with `--sensors-topic`)
- **headers:**
  | name | value |
  | ---- | ----: |
  | row | 9 |
- **payload:**
  ```json
  { "id": "4", "unit": "C", "value": "100" }
  ```

## Build

```
$ mvn clean install
```

## Run

To run a single sensor:

```
$ java -jar target/thing-temperature-sensor-develop-SNAPSHOT.jar \
  -b mqtt+tls://mqtt.demo.tenefit.cloud \
  --state-topic /state \
  --sensors-topic /sensors \
  --control-topic /control \
  --id 1 \
  --row 1
```

To run all sensors:

```
$ scripts/runall.sh mqtt+tls://mqtt.demo.tenefit.cloud
```

To kill all sensors:

```
$ scripts/killall.sh
```

## Sensors end-to-end demo

See [README-sensors-end-to-end-demo](README-sensors-end-to-end-demo.md) for the instructions to run the full sensors demo end-to-end.
