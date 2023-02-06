const https = require('https');
const fs = require('fs');
const util = require('util');

const server = require('./server');

const PORT = 10443;

const options = {
    key: fs.readFileSync(`${__dirname}/certs/server.key`),
    cert: fs.readFileSync(`${__dirname}/certs/server.crt`),
    ca: fs.readFileSync(`${__dirname}/certs/ca.crt`),
    requestCert: true,
    rejectUnauthorized: false,
};

function TestServer(...args) {
    server.TestServer.apply(this, args);

    this.protocol = 'https:';
    this.rootCA = options.ca;
}

util.inherits(TestServer, server.TestServer);

module.exports = server(TestServer, function (dispatcher) {
    return https.createServer(options, dispatcher);
}, PORT);
