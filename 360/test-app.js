'use strict';

const _ = require('lodash');
const nock = require('nock');
const request = require('supertest');

const loadConfig = require('@ps-int/mail-lib/lib/config/load-config.js');
const loadNockDef = _.memoize((filename) => loadConfig(filename));

/**
 * Runs integration test.
 *
 * @param {express} app - Express app.
 * @param {object} config - Test config.
 * @param {string} config.method - HTTP method.
 * @param {string} config.path - Request path.
 * @param {object} config.reqheaders - Request headers.
 * @param {object} config.query - Request query.
 * @param {object} config.body - Request body.
 * @param {string[]} config.nocks - Nock definition locations.
 * @param {string[]} config.headerFilter - Response header filter.
 * @returns {Promise}
 */
function testApp(app, config) {
    const method = config.method.toLowerCase();
    const nockDefs = config.nocks.map(loadNockDef);
    const scopes = nock.define(nockDefs).map((scope, i) => {
        // nock lacks support for `query`, workaround
        if (nockDefs[i].query) {
            scope.interceptors[0].query(nockDefs[i].query);
        }

        if (nockDefs[i].socketDelay) {
            scope.interceptors[0].delayConnection(nockDefs[i].socketDelay);
        }

        return scope;
    });

    return request(app)[method](config.path)
        .set(config.reqheaders || {})
        .query(config.query || {})
        .send(config.body || {})
        .then((response) => {
            const result = {
                headers: _.pick(response.header, config.headerFilter),
                status: response.status,
                body: response.body
            };
            expect(result).toMatchSnapshot();
        })
        .then(() => {
            try {
                scopes.forEach((scope) => scope.done());
            } catch (e) {
                nock.cleanAll();
                throw e;
            }
        });
}

module.exports = testApp;
