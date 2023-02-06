FROM registry.yandex.net/tools/nodejs:4

ENV NPM_CONFIG_CACHE=/dev/shm/npm_cache \
    TMPDIR=/dev/shm/tmp

WORKDIR /project

COPY package.json .

RUN npm install

COPY .enb .enb
COPY gulp gulp

COPY gulpfile.js .borschik .cssautoprefixer ./

CMD npm run test
