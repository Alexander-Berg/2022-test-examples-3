ARG APP_ENV=production
FROM registry.yandex.net/taxi/rtc-baseimage/${APP_ENV}

ARG APP_ENV=production
#COPY mj-test-service-raw/mj-test-service /mj-test-service
RUN apt-get update -qq

ADD *.deb /deb/
RUN apt install -y /deb/*.deb

RUN cp /mj-test-service/supervisor.conf /etc/supervisor/conf.d/mj-test-service.conf

RUN echo '#!/bin/bash' > start-service.sh
RUN echo "/mj-test-service/bin/mj-test-service-start.sh --environment=${APP_ENV}" >> start-service.sh
RUN chmod +x start-service.sh

#RUN apt install default-jdk
#RUN mj-test-service/bin/mj-test-service-start.sh --java-home=/usr/lib/jvm/java-11-openjdk-arm64
#RUN mj-test-service/bin/mj-test-service-start.sh --environment=$APP_ENV

EXPOSE 8080
