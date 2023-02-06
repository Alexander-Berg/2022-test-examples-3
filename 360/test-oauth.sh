#!/bin/sh

secid="sec-01f7jxv1rgtg89nbysebjd7khn"
cid=`yav get version $secid -o client_id`
secret=`yav get version $secid -o client_secret`

echo "username:"
read username
echo "password:"
read password

data=$(curl "https://oauth-test.yandex.ru/token" \
    --silent \
    -X POST \
    -H 'Content-Type: application/x-www-form-urlencoded; charset=UTF-8' \
    -d 'grant_type=password' \
    -d "client_id=$cid" \
    -d "client_secret=$secret" \
    -d "username=$username" \
    -d "password=$password")

echo $data | jq -r .access_token
