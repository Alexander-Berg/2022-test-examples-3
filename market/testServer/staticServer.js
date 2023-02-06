const express = require('express');
const path = require('path');

const app = express();
let server;
const isStaticServerNeeded = process.env.ASSET_PATH === 'http://localhost:8080/';

module.exports = {
    startServer: () => {
        if (!isStaticServerNeeded) {
            return;
        }

        app.use('/js', express.static(path.resolve(process.cwd(), 'build/js')));
        app.use('/', express.static(path.resolve(process.cwd(), 'build')));

        server = app.listen(8080, 'localhost');
    },
    stopServer: () => {
        if (server) {
            server.close();
        }
    },
};
