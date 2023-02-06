FROM registry.yandex.net/rasp/rasp-ubuntu-trusty:6354498

RUN apt-get update
RUN apt-get install -y tzdata
RUN dpkg --compare-versions $(dpkg-query --showformat='${Version;5}' --show tzdata) ge 2020e

WORKDIR /app

RUN mkdir -p /var/log/app

COPY docker/nginx/10-application.conf /etc/nginx/sites-enabled/
COPY docker/testcontext/supervisor/*.conf /etc/supervisor/conf.d/

RUN mkdir -p /app/testcontext/
COPY docker/testcontext/*.yaml /app/testcontext/
COPY cmd/testcontext/testcontext /app/testcontext/
