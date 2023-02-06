/**
 * Если бандл сервера не собран, eslint выбросит ошибку `eslint node/no-missing-require`.
 * Отключаем это правило для файла.
 */
const deepExtend = require('deep-extend');
const testApp = require('./test-app.js');

/**
 * Runs API method test.
 *
 * @function
 * @param {string} path
 * @param {Object} [config] - Test config.
 * @param {string} config.method - HTTP method.
 * @param {string} config.path - Request path.
 * @param {Object} config.reqheaders - Request headers.
 * @param {Object} config.query - Request query.
 * @param {Object} config.body - Request body.
 * @param {string[]} config.nocks - Nock definition locations.
 * @param {string[]} config.headerFilter - Response header filter.
 * @param {Object} config.params - Method params.
 * @returns {Promise}
 */
const testRoute = (path, config = {}) => {
  const app = require('@/app.js');

  config = Object.assign(
    {
      method: 'get',
      nocks: []
    },
    config
  );

  return testApp(
    app,
    deepExtend(
      {
        path,
        reqheaders: {
          cookie:
            'Session_id=TEST_SESSION_ID; i=98765432109876543210; yandexuid=12345678901234567890',
          host: 'localhost',
          'user-agent': 'TEST_USER_AGENT',
          'x-https-request': 'yes',
          'x-original-host': 'TEST.HOST',
          'x-real-ip': '123.123.123.123',
          'x-request-id': 'TEST_REQUEST_ID',
          'X-Requested-With': 'XMLHttpRequest'
        },
        headerFilter: ['content-type']
      },
      config
    )
  );
};

module.exports = testRoute;
