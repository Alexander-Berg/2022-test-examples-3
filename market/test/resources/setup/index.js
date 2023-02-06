const path = require('path');

const Terror = require('terror');

const Resource = require('../../../lib/resource');

const ServiceResource = Resource
    .create()
    .setPath(`${__dirname}/../`)
    .setEnvConfig({
        simple: {
            host: '127.0.0.1',
            timeout: 200,
        },
        'with-cert': {
            host: '127.0.0.1',
            ca: path.resolve(__dirname, '../certs/myRootCA.pem'),
            timeout: 200,
        },
        servant: {
            host: '127.0.0.1',
            ca: path.resolve(__dirname, '../certs/myRootCA.pem'),
            timeout: 200,
        },
    });

// avoid error output to stdio
Terror.prototype.logger = function () {};

module.exports = ServiceResource;
