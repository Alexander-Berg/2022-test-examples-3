FROM registry.yandex.net/mail/mail-trusty-common:latest

COPY docker/ /usr/bin/

RUN apt-get update \
    && apt-get -y install \
        yandex-passport-tvmknife \
        python-msgpack \
        python-six \
        python-nose \
    && apt-get clean

ARG TVM_TICKETS_DIR

ENV TVM_TICKETS_DIR $TVM_TICKETS_DIR

ENTRYPOINT ["/bin/bash", "/usr/bin/entrypoint.sh"]
