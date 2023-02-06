#! /bin/bash

for f in $(find /etc/entrypoint.d/* 2>/dev/null); do
  [[ -f "$f" ]] && { bash "$f" || { echo "entrypoint.d script \"$f\" failed"; exit 1; }; }
done

portoctl run self/sub_nginx \
    respawn=true \
    isolate=false \
    command='/usr/sbin/nginx -c /etc/nginx/nginx.conf -g "daemon off;"'

exec /bin/sleep infinity
