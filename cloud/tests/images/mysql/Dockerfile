FROM registry.yandex.net/dbaas/mysync-test-base:latest
COPY . /var/lib/dist/mysql
COPY ./mysync /usr/bin/mysync
RUN bash /var/lib/dist/mysql/setup.sh
