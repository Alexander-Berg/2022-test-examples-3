FROM registry.yandex.net/rtc-base/bionic:stable

MAINTAINER Sergey Kozlov <serjk86@yandex-team.ru>

RUN echo 'deb http://mirror.yandex.ru/ubuntu/ bionic main restricted universe multiverse' >> /etc/apt/sources.list
RUN echo 'deb http://mirror.yandex.ru/ubuntu/ bionic-updates main restricted universe multiverse' >> /etc/apt/sources.list
RUN apt-get update && \
    apt-get -y install \
    python2.7 python-dev python-pip python-setuptools \
    imagemagick xvfb x11vnc xdotool openbox chromium-browser chromium-chromedriver firefox firefox-geckodriver \
    ffmpeg pulseaudio vlc yandex-porto

RUN adduser --home /work --disabled-password snail

COPY bin/cpproxy /bin/cpproxy
COPY bin/cert_gen /bin/cert_gen
COPY bin/queue_worker /bin/queue_worker
COPY bin/local_test /bin/local_test
COPY bin/log_aggregator /bin/log_aggregator
COPY bin/viewer /bin/viewer
COPY bin/snail_dev_run.sh /bin/snail_dev_run.sh
COPY ru.pak /usr/lib/chromium-browser/locales
WORKDIR /work
CMD [ "/bin/snail_dev_run.sh" ]
