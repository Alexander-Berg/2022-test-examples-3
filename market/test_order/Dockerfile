FROM golang:1.17 AS build
LABEL Author Artem Hitin <hitinap@yandex-team.ru>

RUN mkdir /test_order
ADD . /test_order/
WORKDIR /test_order

ENV TZ=Europe/Moscow

RUN apt-get update && apt-get install -y postgresql-client
RUN mkdir -p ~/.postgresql && wget "https://crls.yandex.net/allCAs.pem" -O ~/.postgresql/root.crt

RUN go get -u github.com/robfig/cron/v3
RUN go get -u github.com/lib/pq

RUN go build -o main .
CMD ["/test_order/main"]

EXPOSE 80 443
