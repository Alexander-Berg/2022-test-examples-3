#!/bin/bash
ipt=`which ip6tables`

$ipt -P INPUT ACCEPT
$ipt -P OUTPUT ACCEPT

$ipt -F INPUT
$ipt -F OUTPUT

$ipt -A INPUT -i lo -j ACCEPT

for host in moddb-mongo01e.yandex.ru	moddb-mongo02e.yandex.ru	moddb-mongo03e.yandex.ru	moddb-mongo04e.yandex.ru	moddb-mongo01f.yandex.ru	moddb-mongo02f.yandex.ru	moddb-mongo03f.yandex.ru	moddb-mongo04f.yandex.ru	moddb-mongo01i.yandex.ru	moddb-mongo02i.yandex.ru	moddb-mongo03i.yandex.ru	moddb-mongo04i.yandex.ru	moddb-mongo01h.yandex.ru	moddb-mongo02h.yandex.ru	moddb-mongo03h.yandex.ru	moddb-mongo04h.yandex.ru ; do
	$ipt -A INPUT -s $host -p tcp -j REJECT
done

for host in moddb-mongo01e.yandex.ru	moddb-mongo02e.yandex.ru	moddb-mongo03e.yandex.ru	moddb-mongo04e.yandex.ru	moddb-mongo01f.yandex.ru	moddb-mongo02f.yandex.ru	moddb-mongo03f.yandex.ru	moddb-mongo04f.yandex.ru	moddb-mongo01i.yandex.ru	moddb-mongo02i.yandex.ru	moddb-mongo03i.yandex.ru	moddb-mongo04i.yandex.ru	moddb-mongo01h.yandex.ru	moddb-mongo02h.yandex.ru	moddb-mongo03h.yandex.ru	moddb-mongo04h.yandex.ru ; do
	$ipt -A OUTPUT -d $host -p tcp -j REJECT
done

$ipt -vnL --line-numbers

