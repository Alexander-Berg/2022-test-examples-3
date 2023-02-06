#!/bin/bash

timetail -t java -n 60 /var/log/sophos.log | /usr/bin/sophos.scanned-stat.py

exit $?
