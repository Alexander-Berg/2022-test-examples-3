#!/usr/bin/env bash
set -xe

source scripts/migrations/migrations.sh

modify_source ADRESA_TEST "user,business.wizard.int01e.tst.maps.yandex.ru,80;" "user,localhost,12000;"
