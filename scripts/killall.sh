#!/bin/bash

kill $(jps -l | grep "thing-temperature-sensor" | cut -d " " -f 1)
