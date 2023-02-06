#!/bin/bash

# --keep-temps to keep output
# --test-stderr to see test progress
ya m -rA --test-stderr --keep-temps "$@"
