#!/bin/bash

export TEST_CONNINFO_PARAMS="user=xiva sslmode=verify-full"
export TEST_CONNINFO="host=pgproxy-test.mail.yandex.net port=6432 dbname=xivadb user=xiva sslmode=verify-full"

nosetests -s xtable.py

# same TEST_CONNINFO

nosetests -s xstore.py
