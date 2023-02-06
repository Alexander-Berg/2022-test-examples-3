#!/bin/bash

source /usr/lib/qloud-mail-mx.sh

[[ -z ${CONFIG_NAME} ]] && CONFIG_NAME=$QLOUD_COMPONENT

# Установка пакетов:
apt-get update -qq;
apt-get install -o Dpkg::Options::="--force-confnew" --force-yes -y $PACKAGES

# Возвращаем права для каталога notsolitesrv:
chmod 755 /etc/notsolitesrv/

# Получаем и разворачиваем секреты:
SECRETS_VERSION="sec-01dq4zy4r5bbeh1q93z5d9pym9"
yav get version $SECRETS_VERSION  -o secrets-fs > /tmp/secrets.tar.gz
tar xzvf /tmp/secrets.tar.gz -C /

# Настройка postfix:
ln -svf /etc/postfix/main.cf-$CONFIG_NAME /etc/postfix/main.cf
maps=$(postconf -h | grep -w hash: | grep -vE "aliases|smtp_scache" | cut -d ':' -f2)
for i in $maps; do
    postmap $i 2>/dev/null
done

# Удаляем старый скрипт получения доменных карт (MAILDLV-3795):
rm /etc/cron.d/get-domain-list

# Инициализация domain_maps:
ln -svf /etc/yandex/domain_maps/configs/maps.conf.$CONFIG_NAME /etc/yandex/domain_maps/maps.conf
/etc/cron.yandex/get-domain-maps.py

# Инициализация related_uids (где релевантно)
if [[ -f /etc/cron.d/get_related_uids && -x /etc/cron.yandex/get_related_uids.sh ]]; then
    /etc/cron.yandex/get_related_uids.sh
fi

# Линкуем конфиги приложений:
echo $PACKAGES | grep -q mxback       && ln -svf /etc/supervisor/conf-available/nwsmtp-out.conf /etc/supervisor/conf.d/nwsmtp-out.conf
echo $PACKAGES | grep -q smtpgate     && ln -svf /etc/supervisor/conf-available/smtpgate.conf /etc/supervisor/conf.d/
echo $PACKAGES | grep -q notsolitesrv && ln -svf /etc/supervisor/conf-available/notsolitesrv.conf /etc/supervisor/conf.d/

/etc/init.d/postfix start

# Отключаем dircnt (MAILDLV-3705):
rm /etc/cron.d/dircnt-postfix

# Права на каталог с логами (MAILDLV-3765):
chmod 775 /var/log/

exec "$@"

