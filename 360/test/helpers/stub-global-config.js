'use strict';

const mockRequire = require('mock-require');

const services = [
    'akita',
    'directory',
    'mbody',
    'meta',
    'settings',
    'tvm'
];

mockRequire('/etc/yamail/u2709-conf.json', {
    services: services.reduce((res, service) => {
        res[service] = {
            url: `http://${service}`,
            methods: { default: { dnsCache: false } }
        };
        return res;
    }, {})
});
