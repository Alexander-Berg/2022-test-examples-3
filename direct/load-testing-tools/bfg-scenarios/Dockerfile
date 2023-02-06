FROM ubuntu:16.04

RUN apt update && apt install -y python python-pip
RUN pip install --upgrade pip setuptools
RUN pip install https://api.github.com/repos/yandex/yandex-tank/tarball/master
