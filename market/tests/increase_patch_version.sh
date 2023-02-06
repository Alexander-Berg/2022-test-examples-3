#!/bin/bash

source common.sh

trap '[ "$?" -eq 0 ] && echo success || echo fail' EXIT

set -ex


[ "$(increase_patch_version '11.22.0')" == '11.22.1' ]
[ "$(increase_patch_version '11.22.33')" == '11.22.34' ]
[ "$(increase_patch_version '11.22')" == '11.22.1' ]
