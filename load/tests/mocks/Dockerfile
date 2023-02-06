# Docker image with mocks for tank tests

FROM registry.yandex.net/rtc-base/xenial:stable
FROM ubuntu:xenial

RUN echo "deb http://load-xenial.dist.yandex.ru/load-xenial stable/all/" >> /etc/apt/sources.list.d/yandex-load.list
RUN echo "deb http://load-xenial.dist.yandex.ru/load-xenial stable/amd64/" >> /etc/apt/sources.list.d/yandex-load.list

RUN export DEBIAN_FRONTEND=noninteractive && \
    apt-get update -q && \
    apt-get install --no-install-recommends -yq \
        iproute2    \
        cron        \
        vim         \
        wget        \
        curl        \
        less        \
        atop        \
        jq          \
	man         \
	net-tools   \
	tcpdump     \
	htop        \
        nginx-extras \
        openssh-client \
        logrotate \
        python-pip && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/* /var/cache/apt/archives/* /tmp/* /var/tmp/*

RUN curl dist.yandex.ru/REPO.asc | apt-key add - && \
    export DEBIAN_FRONTEND=noninteractive && \
    apt-get update -q && \
    apt-get install --no-install-recommends -yq --allow-unauthenticated \
        load-grpc-test-server && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/* /var/cache/apt/archives/* /tmp/* /var/tmp/*

RUN export DEBIAN_FRONTEND=noninteractive && \
    apt-get update -q && \
    apt-get install --no-install-recommends -yq \
        python-dev \
        python-setuptools \
        build-essential && \
    python -m pip install --upgrade "pip < 21.0"&& \
    python -m pip install --upgrade setuptools wheel && \
    python -m pip install tornado && \
    python -m pip install requests && \
    python -m pip install msgpack-python && \
    python -m pip install supervisor && \
    python -m pip install psutil && \
    apt-get autoremove -y python-dev build-essential && \
    apt-get clean && \
    rm -rf /root/.cache/* /var/lib/apt/lists/* /var/cache/apt/archives/* /tmp/* /var/tmp/* /etc/nginx/sites-enabled/default && \
    mkdir -p /var/log/supervisor && \
    mkdir -p /var/lib/logrotate && \
    mkdir -p /var/spool/nginx/cache && mkdir -p /var/log/nginx

COPY configs/.inputrc /root/
COPY configs/.bashrc /root/
COPY configs/.vimrc /root/

COPY configs/logrotate /etc/cron.daily/logrotate
COPY configs/nginx.conf         /etc/nginx/
COPY configs/sites-enabled/*    /etc/nginx/sites-enabled/
COPY configs/supervisord.conf   /etc/supervisord.conf
COPY configs/nginx_logrotate    /etc/logrotate.d/nginx

EXPOSE 443

ENV HOME=/root

ENTRYPOINT ["/usr/local/bin/supervisord", "-c", "/etc/supervisord.conf"] 
