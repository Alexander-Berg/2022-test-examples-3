'use strict';

const { Core } = require('@yandex-int/duffman');

const data = require('../../index.js');
const Config = require('./test-config.js');

class TestCore extends Core {
    constructor(req, res) {
        super(req, res);

        this.models = data.models;
        this.services = data.services;
        this.config = new Config(this);
    }

    getServiceOptions(serviceName, method) {
        const getter = this.config.getServiceOptionsGetter(serviceName);

        if (typeof method === 'string') {
            return getter(method);
        }

        return getter;
    }
}

module.exports = TestCore;
