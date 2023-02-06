const url = require('url');

/**
 * @param {string} expectedPath
 * @param {number} [timeout] - Таймаут в миллисекундах
 * @returns {Promise}
 */
module.exports = async function yaWaitForPage(
  expectedPath,
  timeout = undefined
) {
  if (typeof timeout !== 'number') {
    timeout = this.options.waitforTimeout;
  }

  const errorMessage = `page ${expectedPath} still not visible after ${timeout}ms`;

  return this.waitUntil(
    () => {
      return this.getUrl().then(actualUrl => {
        const actualUrlObject = new url.URL(actualUrl);
        const actualPath = actualUrlObject.pathname;

        return actualPath === expectedPath;
      });
    },
    timeout,
    errorMessage
  );
};
