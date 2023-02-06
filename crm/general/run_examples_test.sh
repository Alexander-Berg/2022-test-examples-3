#!/bin/sh
ya make > /dev/null
echo 'Compiled!'
for example in examples/*; do echo "Run with ${example}"; ./changelog run --input "`cat ${example}`" --test --sb-schema Changelog; done

# stolen from https://a.yandex-team.ru/arc/trunk/arcadia/billing/tasklets/changelog/run_examples.sh?rev=r8493308
