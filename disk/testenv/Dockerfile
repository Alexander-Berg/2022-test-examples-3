FROM ubuntu:trusty
MAINTAINER swined@yandex-team.ru

ENV DEBIAN_FRONTEND noninteractive
ADD yandex.list /etc/apt/sources.list.d/
RUN apt-get update --allow-unauthenticated
RUN apt-get install -y --allow-unauthenticated uatraits-perl uatraits-data libtimedate-perl libdatetime-perl liburi-perl libyaml-tiny-perl libcrypt-rijndael-perl

RUN mkdir /etc/yandex
RUN echo test > /etc/yandex/environment.type

CMD ["bash"]