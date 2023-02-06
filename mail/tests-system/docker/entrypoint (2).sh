#! /bin/bash

echo "$TEST_TVM_SECRET" > /etc/collectors-ext/tvm_secret

if [[ "${TEST_ENV}" == "import" ]]; then
    /usr/sbin/collectors-ext /etc/collectors-ext/dev-import.yml &
    while true; do
        /etc/cron.yandex/reload-tokens.sh > /var/log/token_run.log 2>&1
        sleep 2
    done
else
    /usr/sbin/collectors-ext /etc/collectors-ext/dev.yml
fi
