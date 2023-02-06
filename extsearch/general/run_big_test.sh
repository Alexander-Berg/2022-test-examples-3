#!/usr/bin/env bash

ya make -trAP --test-stderr --keep-temps --test-param=run_with_yt --test-param=save_regression_test_tables=True "$@" 

