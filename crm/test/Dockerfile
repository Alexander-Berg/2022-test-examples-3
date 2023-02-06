FROM registry.yandex.net/crm/oraclient11:latest

ARG APP_DIR=/app/
WORKDIR $APP_DIR

RUN mkdir -p $APP_DIR
COPY ./build/tnsnames.ora $ORACLE_HOME/network/admin
COPY ./build/crm_test $APP_DIR/crm_test
COPY ./build/src/ $APP_DIR/

RUN apt-get -qq update && apt-get -qq install --no-install-recommends -y \
    git \
    curl \
    make \
    unzip \
    build-essential \
    yandex-jdk8

RUN rm -rf /var/lib/apt/lists/*

# Install and setup teamcity agent
ARG SERVER_URL=https://teamcity.yandex-team.ru
ENV CONFIG_DIR=/data/teamcity_agent/conf
ENV AGENT_HOME=/opt/buildagent
RUN wget -q --output-document=buildagent.zip \
    ${SERVER_URL}/update/buildAgent.zip && \
    unzip -qq buildagent.zip -d ${AGENT_HOME} && \
    rm -rf buildagent.zip && \
    chmod u+x ${AGENT_HOME}/bin/*.sh

COPY entrypoint.sh /
RUN chmod u+x /entrypoint.sh
ENTRYPOINT ["/entrypoint.sh"]
EXPOSE 9090