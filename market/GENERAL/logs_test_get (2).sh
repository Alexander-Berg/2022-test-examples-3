#!/bin/sh

[ -z "$TMPDIR" ] && TMPDIR='/tmp'
log_dir="${TMPDIR%/}/rlogs"

[ -d "$log_dir" ] && rm -rf "$log_dir"
mkdir -p "$log_dir"

while read -r host; do
  [ -z "$host" ] && continue
  sftp "$host:/logs/autoorder/autoorder.log" "$log_dir/$host.log" &
done << EOF
  testing-market-autoorder-sas-3.sas.yp-c.yandex.net
  testing-market-autoorder-vla-3.vla.yp-c.yandex.net
EOF

wait
