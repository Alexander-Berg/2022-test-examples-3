FROM registry.yandex.net/mail/mail-xenial-common:latest

RUN apt-get update \
    && apt-get -y install \
    nginx-extras \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

RUN rm /etc/nginx/sites-enabled/default

ARG   deploydir
COPY  $deploydir /

RUN chmod 744 /usr/sbin/*
CMD ["/usr/sbin/entrypoint.sh"]
