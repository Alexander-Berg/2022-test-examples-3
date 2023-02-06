#!/bin/sh
# script for manual testing

# testing
# https://yav.yandex-team.ru/secret/sec-01f7x59v78addkgsrdwyfebefd/explore/version/ver-01f7x59v7ksw4t974cp3w4n0bf
ya tool tvmknife get_service_ticket client_credentials --src 2028634 --dst 2010068
ya tool tvmknife get_service_ticket client_credentials --src 2028634 --dst 2011222

# stable
# https://yav.yandex-team.ru/secret/sec-01f7x5ahrh277e40p59q88rd6w/explore/version/ver-01f7x5ahvk35138jmy23ggy9t8
ya tool tvmknife get_service_ticket client_credentials --src 2028636 --dst 2010064
ya tool tvmknife get_service_ticket client_credentials --src 2028636 --dst 2011220
