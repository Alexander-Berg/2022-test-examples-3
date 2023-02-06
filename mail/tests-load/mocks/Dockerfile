FROM registry.yandex.net/mail/mail-xenial-common:latest

RUN apt-get update
RUN apt-get install -y nginx-extras python3-pip

RUN rm /etc/nginx/sites-enabled/default

RUN pip3 install sanic

COPY . /

RUN chmod 744 /usr/sbin/*
RUN chmod 744 /usr/bin/*
CMD ["/usr/sbin/entrypoint.sh"]
