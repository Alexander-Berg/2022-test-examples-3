#! /bin/bash

portoctl run self/sub_smtp \
    respawn=true \
    isolate=false \
    stdout_path="/var/log/smtp_mock.out" \
    stderr_path="/var/log/smtp_mock.err" \
    command='/usr/sbin/smtp_mock.py 9025'
