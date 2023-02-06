#!/bin/bash
set -x

../yandex-du-rfsd-generate-acls.py --config-dir ./rfs-exports.d --swap-dir ./swap -f ./rfs-exports --debug

