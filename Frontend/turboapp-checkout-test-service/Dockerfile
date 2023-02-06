FROM registry.yandex.net/toolbox/nodejs:12.13.0-bionic

COPY package.json /app/
COPY package-lock.json /app/
COPY server/build /app/

RUN (cd /app && npm ci --production --registry https://npm.yandex-team.ru) && npm cache clean --force

ARG APP_VERSION

ENV APP_VERSION=${APP_VERSION} \
    CFG_DIR=/app/server/configs

CMD node /app/server/index.js
