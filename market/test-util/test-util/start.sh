#!/bin/sh

USER=checkout
DAEMON=market-checkout-test-util
LOG=/var/log/${USER}/${DAEMON}.log


RESULT=$(cat /etc/yandex/environment.type)

if [[ "x${RESULT}" == "xproduction" ]]; then
	elliptics_url="http://cs-elliptics.yandex.net:88/get/market/mbi/shopOutlet"
	echo "Elliptics url: $elliptics_url"
	current_file=$(curl -s "${elliptics_url}/meta" | awk -F ' ' '{print $2}')
	echo "Downloading outlets from elliptics: ${elliptics_url}/${current_file}"
	curl -s "${elliptics_url}/${current_file}" \
	| xmllint --format - \
	| grep "<?xml\|<OutletInfo\|<Shop id\|<outlet>\|<ShopPointId>\|<RegionId>\|</outlet>\|</Shop>\|</OutletInfo" \
	> /var/lib/yandex/${DAEMON}/shopsOutlet.xml
else
	URL="http://mi01ht:3131/shopsOutlet.xml"
	echo "Downloading outlets from: ${URL}"
	curl -s "${URL}" \
    | xmllint --format - \
    | grep "<?xml\|<OutletInfo\|<Shop id\|<outlet>\|<ShopPointId>\|<RegionId>\|</outlet>\|</Shop>\|</OutletInfo" \
    > /var/lib/yandex/${DAEMON}/shopsOutlet.xml
fi

cd /usr/lib/yandex/${DAEMON}/
/usr/bin/python test-util.py >> ${LOG} 2>&1 &

echo $! > /var/run/${USER}/${DAEMON}.pid
