#!/bin/sh

[ -z "$TMPDIR" ] && TMPDIR='/tmp'
log_dir="${TMPDIR%/}/pmlogs"

[ -d "$log_dir" ] && rm -rf "$log_dir"
mkdir -p "$log_dir"

while read -r host; do
  [ -z "$host" ] && continue
  sftp -o 'StrictHostKeyChecking=no' "root@$host:/var/log/yandex/pricing-mgmt/pricing-mgmt.log" "$log_dir/$host.log"
done << EOF
  pricing-mgmt_box.dbbfrtrtsqmlqeoa.sas.yp-c.yandex.net
  pricing-mgmt_box.kku4gvkxbujmizqq.vla.yp-c.yandex.net
EOF
