#!/bin/bash -e

export UNITTEST=1
python -m unittest discover -v -s $(dirname "$0") -p '*.py' $*
