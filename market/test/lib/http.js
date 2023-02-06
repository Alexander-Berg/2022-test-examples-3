const http = require('http');

const server = require('./server');

const PORT = 10080;

module.exports = server(server.TestServer, function (dispatcher) {
    return http.createServer(dispatcher);
}, PORT);
