/* eslint-disable import/no-extraneous-dependencies */

let Terror = require('terror');
let Resource = require('../../../lib/resource');
let ServiceResource = Resource
    .create()
    .setPath(__dirname + '/../')
    .setEnvConfig({
        simple: {
            host: '127.0.0.1',
            timeout: 200,
        },
    });

// avoid error output to stdio
Terror.prototype.logger = function() {};

module.exports = ServiceResource;
