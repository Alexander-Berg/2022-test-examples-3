FROM registry.yandex.net/ubuntu:bionic

ENV TZ=Europe/Moscow

ARG CLICKHOUSE_VERSION
ARG NETWORK_NAME

RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone && \
    apt-get update -qq && \
    apt-get install -y \
        apt-transport-https \
        tzdata \
        locales \
        openssh-server \
        supervisor \
        sudo && \
    echo 'en_US.UTF-8 UTF-8' > /etc/locale.gen && \
    locale-gen

ENV LANG en_US.UTF-8
ENV LANGUAGE en_US:en
ENV LC_ALL en_US.UTF-8

# setup ssh for debugging
RUN echo "root:root" | chpasswd && \
    sed -i -e '/PermitRootLogin/ s/.*/PermitRootLogin yes/' /etc/ssh/sshd_config && \
    mkdir /var/run/sshd

RUN mkdir -p /etc/apt/sources.list.d && \
    echo "deb http://dist.yandex.ru/mdb-bionic-secure testing/all/"     | tee /etc/apt/sources.list.d/mdb-bionic-secure-testing.list && \
    echo "deb http://dist.yandex.ru/mdb-bionic-secure testing/\$(ARCH)/" | tee -a /etc/apt/sources.list.d/mdb-bionic-secure-testing.list && \
    echo "deb http://dist.yandex.ru/mdb-bionic-secure stable/all/"      | tee /etc/apt/sources.list.d/mdb-bionic-secure-stable.list && \
    echo "deb http://dist.yandex.ru/mdb-bionic-secure stable/\$(ARCH)/"  | tee -a /etc/apt/sources.list.d/mdb-bionic-secure-stable.list && \
    apt-get update -qq && \
    apt-get upgrade -y && \
    DEBIAN_FRONTEND=noninteractive apt-get install -y \
        clickhouse-common-static=$CLICKHOUSE_VERSION \
        clickhouse-server=$CLICKHOUSE_VERSION \
        clickhouse-client=$CLICKHOUSE_VERSION \
        ch-backup=2.0.344 && \
    DEBIAN_FRONTEND=noninteractive apt-get install -y clickhouse-geodb && \
    DEBIAN_FRONTEND=noninteractive apt-get install -y yandex-timetail && \
    rm -rf /var/lib/apt/lists/* /var/cache/debconf && \
    apt-get clean

RUN mkdir /etc/clickhouse-server/config.d || true
RUN chown clickhouse:clickhouse /etc/clickhouse-server/config.d
RUN mkdir /var/log/monrun

COPY staging/tools/ /tmp/tools/
RUN tar -x -f /tmp/tools/mdb-ch-tools.*.tar.gz -C /

RUN rm -rf /etc/supervisor && \
    ln --force -s /config/supervisor /etc/supervisor && \
    ln -s /config/dbaas.conf /etc/dbaas.conf && \
    mkdir -p /etc/yandex/ch-backup && \
    ln -s /config/ch-backup.conf /etc/yandex/ch-backup/ch-backup.conf

RUN mkdir -p /etc/clickhouse-server/ssl && \
    openssl req -subj "/CN=localhost" -new -newkey rsa:2048 -days 365 -nodes -x509 \
        -keyout /etc/clickhouse-server/ssl/server.key \
        -addext "subjectAltName=DNS:clickhouse01,DNS:clickhouse02,DNS:clickhouse01.$NETWORK_NAME,DNS:clickhouse02.$NETWORK_NAME" \
        -out /etc/clickhouse-server/ssl/server.crt && \
    ln -s /etc/clickhouse-server/ssl/server.crt /etc/clickhouse-server/ssl/allCAs.pem && \
    mkdir -p /etc/clickhouse-server/conf.d && \
    ln -s /config/clickhouse-server.xml /etc/clickhouse-server/conf.d/ && \
    chown -R clickhouse:clickhouse /etc/clickhouse-server/ /usr/bin/clickhouse

COPY images/clickhouse/config/users.xml /etc/clickhouse-server/users.xml
COPY images/clickhouse/config/monitor-ch-backup /etc/sudoers.d/monitor-ch-backup

EXPOSE 8123 8443 9000 9440

CMD ["supervisord", "-c", "/etc/supervisor/supervisord.conf"]

RUN groupadd monitor && useradd -g monitor -G monitor,clickhouse monitor && \
    chown monitor:monitor -R /var/log/clickhouse-monitoring && \
    chmod 755 /var/lib/clickhouse && \
    chmod 777 /etc && \
    chmod 755 /etc/clickhouse-server && \
    chmod 750 /etc/clickhouse-server/ssl && \
    chmod 640 /etc/clickhouse-server/ssl/server.crt && \
    chmod 640 /etc/clickhouse-server/ssl/allCAs.pem
