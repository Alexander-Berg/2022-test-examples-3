#!/bin/bash

master_sign_pub=/etc/salt/pki/minion/master_sign.pub

if [[ "$(service salt-minion status 2>&1)" =~ start ]];then
   msg="2;salt-minion running"
else
   msg="0;Ok"
fi


# if failover enabled
if egrep -q '^[^#]*master_type[^#]+failover' /etc/salt/minion; then
   # and master_sign.pub not exists
   if ! test -e $master_sign_pub; then
      msg="2;failover enabled without master_sign.pub${msg#[0-9]}"
   elif ! openssl rsa -noout -text -pubin -in $master_sign_pub &>/dev/null; then
      msg="2;failed to load master_sign.pub${msg#[0-9]}"
   fi
fi

echo "${msg:-0;ok}"
