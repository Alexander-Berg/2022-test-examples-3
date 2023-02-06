#!/bin/bash

#SETUP: очистка логов пред тестами
rm -rf /var/log/mpfs-api-admin/*

cd test/api_admin

py.test -v --junit-xml=api-admin-report.xml -n 2
