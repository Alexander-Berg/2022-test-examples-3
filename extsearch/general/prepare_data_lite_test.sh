#!/bin/bash
cp conf/nginx-prod.conf conf/nginx.conf
if [ -f data/loopsinfo.pb -o -f data/pregenerated.pb ]; then
    echo Heavy data found!
    exit 1
fi
mkdir ssl
ya vault get version -o 7F001295D630B69E2A7FC57F930002001295D6_certificate ver-01ewfvgf6k1ndgzwfwwbde8n5x >ssl/cert.pem || exit 1
ya vault get version -o 7F001295D630B69E2A7FC57F930002001295D6_private_key ver-01ewfvgf6k1ndgzwfwwbde8n5x >ssl/key.pem || exit 1
cp conf/nginx-test.conf conf/nginx.conf
ya upload --ttl=365 -T GENERATIVE_MUSIC_TEST_DATA_LITE --tar ssl data conf html generative_daemon_testing.json
rm ssl/cert.pem
rm ssl/key.pem
rmdir ssl
rm conf/nginx.conf
