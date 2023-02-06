FROM registry.yandex.net/ubuntu:xenial

# Устанавливаем бинарные зависимости
RUN apt-get update \
    && apt-get install -y --force-yes \
        gettext-base \
        python2.7 \
        python-pip \
        python-virtualenv \
        python-setuptools \
        gcc \
        git \
        tcpdump \
        lsof \
        telnet \
        htop \
        iotop \
        mc \
        ncdu \
        dnsutils

# Генерим необходимые локали
RUN locale-gen ru_RU.UTF-8 \
    && locale-gen tr_TR.UTF-8 \
    && locale-gen en_GB.UTF-8 \
    && update-locale
ENV LANG=ru_RU.UTF-8

# pip
RUN mkdir -p /root/.pip
COPY pip.conf /root/.pip/

# Ставим новейший pip
RUN pip -q install --upgrade \
    pip~=9.0 \
    setuptools

# Устанавливаем 3-й питон
RUN apt-get install software-properties-common -y && \
    add-apt-repository ppa:deadsnakes/ppa -y && \
    apt-get update && \
    apt-get install python3.7-dev -y && \
    apt-get install python3-pip -y && \
    python3.7 -m pip install --upgrade virtualenv && \
    rm /usr/lib/python3/dist-packages/virtualenv.py

WORKDIR /app
RUN virtualenv -p python3.7 .env
COPY requirements.txt /app/
RUN .env/bin/python3.7 -mpip install -r requirements.txt

COPY ./smoke_tests /app/smoke_tests
RUN chmod +x /app/smoke_tests/run.sh

WORKDIR /app/smoke_tests
ENV PYTHONPATH=/app:/app/smoke_tests
#ENV PY_IGNORE_IMPORTMISMATCH=1
ENV REQUESTS_CA_BUNDLE=/app/smoke_tests/certs.pem

ENTRYPOINT ["/app/smoke_tests/run.sh"]
CMD ["-n20", "-sv", "-ra", "--env=production", "--config=morda_front"]
