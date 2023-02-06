#!/usr/bin/env bash
# получаем список измененных блоков и экспортируем в переменную окружения

npx selective configure > selective-examples.json

SEL_SUITES=$(npx selective transform --file=selective-examples.json --separator=' ' --target=examples)


eval echo -n $SEL_SUITES
