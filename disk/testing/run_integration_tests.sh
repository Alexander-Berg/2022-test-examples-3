#!/bin/bash

tools/testing/setup_before_tests.sh

tools/testing/run_mpfs_tests.sh
tools/testing/run_platform_tests.sh
