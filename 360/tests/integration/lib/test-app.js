/**
 * @see https://github.yandex-team.ru/Daria/web-api/blob/dev/integration/lib/test-app.js
 */

'use strict';

const _ = require('lodash');
const nock = require('nock');
const request = require('supertest');
const loadConfig = require('mail-internal-api-lib/lib/config/load-config.js');

const loadNockDef = _.memoize(filename => loadConfig(filename));

/**
 * Runs integration test.
 *
 * @function
 * @param {express} app - Express app.
 * @param {Object} config - Test config.
 * @param {string} config.method - HTTP method.
 * @param {string} config.path - Request path.
 * @param {Object} config.reqheaders - Request headers.
 * @param {Object} config.query - Request query.
 * @param {Object} config.body - Request body.
 * @param {string[]} config.nocks - Nock definition locations.
 * @param {string[]} config.headerFilter - Response header filter.
 * @returns {Promise}
 */
const testApp = (app, config) => {
  const method = config.method.toLowerCase();
  const nockDefs = config.nocks.map(loadNockDef);
  const scopes = nock.define(nockDefs).map((scope, i) => {
    // nock lacks support for `query`, workaround
    scope.interceptors[0].query(nockDefs[i].query);

    if (nockDefs[i].socketDelay) {
      scope.interceptors[0].socketDelay(nockDefs[i].socketDelay);
    }

    return scope;
  });

  const requestMethod = request(app)[method];

  return requestMethod(config.path)
    .set(config.reqheaders)
    .query(config.query)
    .send(config.body)
    .then(response => {
      const result = {
        headers: _.pick(response.header, config.headerFilter),
        status: response.status,
        body: response.body
      };

      expect(result).toMatchSnapshot();
    })
    .then(() => {
      try {
        scopes.forEach(scope => scope.done());
      } catch (error) {
        nock.cleanAll();

        throw error;
      }
    });
};

module.exports = testApp;
