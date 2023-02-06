'use strict';

const mock = require('mock-require');

const tvmMock = function(req, res, next) {
    const services = [
        'aceventura',
        'akita',
        'collie',
        'directory',
        'mbody',
        'meta',
        'mlp',
        'mops',
        'msearch',
        'sendbernar',
        'settings',
        'sheltie',
        'staff-api'
    ];

    req.tvm = {
        tickets: services.reduce((tickets, service) => {
            tickets[service] = { ticket: `tvm-service-ticket-${service}` };
            return tickets;
        }, {}),
        headers: services.reduce((headers, service) => {
            headers[`tvm-service-${service}`] = `tvm-service-ticket-${service}`;
            return headers;
        }, {})
    };
    next();
};

mock(require.resolve('../../routes/middlewares/tvm2'), tvmMock);
