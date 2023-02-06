#!/usr/bin/env bash

curl -o "sandbox.tar.gz" -SL "https://proxy.sandbox.yandex-team.ru/last/SANDBOX_ARCHIVE?attrs=%7B%22released%22:%20%22stable%22,%20%22type%22:%20%22library%22%7D"

tar -xzf "sandbox.tar.gz"

rm sandbox.tar.gz
