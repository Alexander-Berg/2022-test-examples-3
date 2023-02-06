#!/bin/bash

set -e

docker ps -a | grep registry.yandex.net/direct/dbschema: | awk '{print $1}' | xargs docker rm -fv
