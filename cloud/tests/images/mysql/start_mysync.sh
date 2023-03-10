mkdir -p /var/run/mysync

eval "cat <<EOF
$(</var/lib/dist/mysql/mysync.yaml)
EOF
" 2>/dev/null >/etc/mysync.yaml

if [ ! -f /tmp/usedspace ]; then
    echo 10 > /tmp/usedspace
fi
exec /usr/bin/mysync --loglevel=Debug
