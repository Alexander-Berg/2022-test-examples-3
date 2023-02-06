#!/bin/bash

python3 -m unittest discover -s `dirname "$0"`/me_lib/ -p '*.py' $*
