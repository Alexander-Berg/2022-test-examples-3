'use strict';

const qs = require('querystring');
const _memoize = require('lodash/memoize');

const { loadConfig } = require('@ps-int/mail-lib/moe');
const nockRoot = require('path').resolve(__dirname, '../../__nocks__');

const loadNockDef = _memoize((filename) => {
    const config = loadConfig(`${nockRoot}/${filename}`);
    if (config.query) {
        config.path += '?' + qs.stringify(config.query);
        delete config.query;
    }
    return config;
});

function loadNockDefs(paths = [], nock) {
    const nockDefs = paths.map(loadNockDef);
    const scopes = nock.define(nockDefs).map((scope, i) => {
        const def = nockDefs[i];
        const interceptor = scope.interceptors[0];

        if (def.socketDelay) {
            interceptor.socketDelay(def.socketDelay);
        }
        if (def.delay) {
            interceptor.delay(def.delay);
        }

        return scope;
    });
    return scopes;
}

module.exports = loadNockDefs;
