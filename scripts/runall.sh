#!/bin/bash

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"

PIDS=""

startSensor() {
  local ID=$1
  local ROW=$2

  if [ -z ${3+x} ]; then
    local STATE=""
  else
    local STATE="--state $3"
  fi

  java -jar target/thing-temperature-sensor-develop-SNAPSHOT.jar \
    -b ${ADDRESS} \
    --state-topic /state \
    --sensors-topic /sensors \
    --control-topic /control \
    --id ${ID} \
    --row ${ROW} \
    ${STATE} &

  PIDS="${PIDS} $!"
}

if [ -z ${1+x} ]; then
  echo "ERROR: You need to specify the address. For example: mqtt+tls://mqtt.example.com"
  exit 1
else
  ADDRESS=${1}
fi

startSensor 1 1
startSensor 2 1 OFF
startSensor 3 2
startSensor 4 2 OFF
startSensor 5 3
startSensor 6 3
startSensor 7 3
startSensor 8 4
startSensor 9 4 OFF
startSensor 10 4

echo ""
echo "To see sensor processes:"
echo ""
echo "  ps -ef | grep thing-temperature-sensor"
echo ""
echo "To shutdown sensors:"
echo ""
echo "  kill -9 ${PIDS}"
echo ""
echo "or:"
echo ""
echo "  scripts/killall.sh"
echo ""
