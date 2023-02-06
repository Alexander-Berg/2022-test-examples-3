#!/bin/bash

TCKT=`ya tool tvmknife get_service_ticket sshkey  --src 2000433 --dst 2000433 2>/dev/null`
ROOT=`ya tool tvmknife get_service_ticket sshkey  --src 2002458 --dst 2000433 2>/dev/null`

./example config.yml &
PID="$!"
sleep 1

curl -H "X-Ya-Service-Ticket: $TCKT" 'http://localhost:14488/dummy_handler'; echo ''
curl -H "X-Ya-Service-Ticket: $TCKT" 'http://localhost:14488/usual_handler'; echo ''
curl -H "X-Ya-Service-Ticket: $ROOT" 'http://localhost:14488/usual_handler_with_additional_tvm_info'; echo ''
curl -H "X-Ya-Service-Ticket: $TCKT" 'http://localhost:14488/custom_logic_handler'; echo ''
curl -H "X-Ya-Service-Ticket: $TCKT" 'http://localhost:14488/custom_logic_handler2'; echo ''


kill "$PID"
