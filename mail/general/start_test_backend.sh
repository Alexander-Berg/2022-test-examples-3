#!/bin/bash

gunicorn -D \
        -b 127.0.0.1:8001 \
        --workers=4 \
        --access-logfile /var/log/settings/access.log \
        --log-file /var/log/settings/error.log \
        settings \
    && \
/usr/sbin/nginx -c /etc/nginx/nginx.conf
