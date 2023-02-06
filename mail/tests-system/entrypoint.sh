#! /bin/bash

/usr/sbin/fake_apns_server /etc/fake_apns_server/cert.pem 2> /var/log/fake_apns_server.log &
/app/app /app/config/local-autotest.yml
