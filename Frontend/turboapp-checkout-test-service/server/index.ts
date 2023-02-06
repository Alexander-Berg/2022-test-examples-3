/// <reference path='./configs/types.ts' />

import http from 'http';
import config from '@yandex-int/yandex-cfg';

import serverUpgrade from './lib/server-upgrade';

const server = http.createServer();

server.on('upgrade', serverUpgrade);

server.listen(config.server.port, () => {
    // eslint-disable-next-line no-console
    console.info(`Listening on port ${config.server.port}`);
});

module.exports = server;
