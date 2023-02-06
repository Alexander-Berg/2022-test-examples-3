#! /bin/bash

setup_mds_messages.py

portoctl run self/sub_passport \
    respawn=true \
    isolate=false \
    command="/usr/sbin/passport.py" \

portoctl run self/sub_nginx \
    respawn=true \
    isolate=false \
    command='/usr/sbin/nginx -c /etc/nginx/nginx.conf -g "daemon off;"'

exec /bin/sleep infinity
