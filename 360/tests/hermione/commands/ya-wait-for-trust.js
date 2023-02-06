const url = require('url');

/**
 * @param {number} [timeout] - Таймаут в миллисекундах
 * @returns {Promise}
 */
module.exports = async function yaWaitForTrust(timeout = undefined) {
  if (typeof timeout !== 'number') {
    timeout = this.options.waitforTimeout;
  }

  const errorMessage = `the Yandex.Trust service page still not visible after ${timeout}ms`;

  return this.waitUntil(
    () => {
      return this.getUrl().then(actualUrl => {
        const actualUrlObject = new url.URL(actualUrl);
        const actualHost = actualUrlObject.host;

        return actualHost.startsWith('trust');
      });
    },
    timeout,
    errorMessage
  );
};
