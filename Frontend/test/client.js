var config = require('./config');
var client = require('..').init({ token: config.token, endpoint: config.endpoint });

module.exports = client;
