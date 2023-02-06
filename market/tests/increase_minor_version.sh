#!/bin/bash

source common.sh

trap '[ "$?" -eq 0 ] && echo success || echo fail' EXIT

set -ex


[ "$(increase_minor_version '11.22.0')" == '11.23.0' ]
[ "$(increase_minor_version '11.22.33')" == '11.23.0' ]
[ "$(increase_minor_version '11.22')" == '11.23.0' ]
