#!/usr/bin/env bash
set -ex

cd $(dirname "${BASH_SOURCE[0]}")/..
ya make --yt-store -tt
