FROM registry.yandex.net/ubuntu:xenial

MAINTAINER Dmitrii Latyshev <dddlatyshev@yandex-team.ru>

#
# initial system setup
#

ENV timezone=Europe/Moscow

ENV LANG=C
ENV LC_ALL=$LANG
ENV LANGUAGE=en
ENV DEBIAN_FRONTEND=noninteractive

# initial configuration
RUN echo "### Initial configuration..." && \
    export LANG=C && export LC_ALL=C && \
    rm -f /etc/localtime /etc/locale.gen && \
    echo > /etc/locale.gen && \
    echo -e "LANG=$LANG\nLC_ALL=$LANG\nLANGUAGE=$LANGUAGE" > /etc/default/locale && \
    dpkg-reconfigure locales && \
    echo "$timezone" > /etc/timezone && \
    dpkg-reconfigure tzdata

# install some stuff
RUN apt-get -q=2 update && \
    apt-get -q=2 install apt-utils && \
    apt-get -q=2 upgrade && \
    apt-get -q=2 install yandex-search-common-apt && \
    apt-get -q=2 update && \
    apt-get -q=2 install yandex-repo-yt-common && \
    apt-get -q=2 update

RUN apt-get install -q=2 \
      jq \
      m4 \
      net-tools \
      openssh-client \
      python2.7 \
      python-flask \
      python-pip \
      tcpdump \
      telnet \
      time

# install packages
RUN apt-get -q=2 install \
      yandex-search-cm-master \
      yandex-search-cm-solver \
      yandex-search-cm-worker \
      yandex-yt-python \
      yandex-yt-python-tools \
      yandex-yt-python-yson \
      yandex-yt-transfer-manager-client

EXPOSE 80

RUN ln -s /ephemeral/work /Berkanavt/work && \
    ln -s /ephemeral/clustermaster-var /Berkanavt/clustermaster/var

ENTRYPOINT /entrypoint
