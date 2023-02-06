const got = require('got');
const _get = require('lodash/get');

const ENTITY_ID = 'frontpay-autotests-testing';

/**
 * @typedef {import('got').Got} Got
 * @typedef {import('got').Options} GotOptions
 * @typedef {import('got').Response} GotResponse
 */

/**
 * @function
 * @param {Error & { response: GotResponse | void }} error
 * @returns {string}
 */
const getRequestErrorMessage = error => {
  const { response } = error;
  const messageParts = [error.message];

  if (response) {
    const responseBody = _get(response, 'body');
    const requestOptions = _get(response, 'request.options');
    const responseJson = JSON.stringify(responseBody, null, 2);
    const requestJson = JSON.stringify(requestOptions, null, 2);

    messageParts.push(
      'Response body:',
      responseJson,
      '',
      'Request options:',
      requestJson
    );
  }

  return messageParts.join('\n');
};

/**
 * @function
 * @param {Object} browser
 * @param {string} name
 * @returns {Promise<string|undefined>}
 */
const getCookieString = async (browser, name) => {
  const cookie = await browser.getCookie(name);

  if (!cookie) {
    return null;
  }

  return `${name}=${cookie.value}`;
};

/**
 * @function
 * @param {Object} browser
 * @param {string[]} names
 * @returns {Promise<string>}
 */
const getCookiesString = async (browser, names) => {
  const cookiesPromises = names.map(name => getCookieString(browser, name));
  const cookies = await Promise.all(cookiesPromises);

  return cookies.filter(Boolean).join(';');
};

/**
 * @function
 * @param {Object} browser
 * @returns {Got}
 */
const makeTransport = async browser => {
  const { baseUrl } = browser.options;
  const cookies = await getCookiesString(browser, [
    'Session_id',
    'sessionid2',
    'merchant_id'
  ]);

  return got.extend({
    method: 'get',
    prefixUrl: baseUrl,
    responseType: 'json',
    resolveBodyOnly: true,
    retries: 2,
    headers: {
      'X-Requested-With': 'XMLHttpRequest',
      'X-Ya-Entity-Id': ENTITY_ID,
      Cookie: cookies
    }
  });
};

/**
 * @function
 * @param {string} url
 * @param {GotOptions} [options]
 * @returns {Promise<any>}
 */
module.exports = async function yaApiRequest(url, options = {}) {
  try {
    const transport = await makeTransport(this);
    const response = await transport(url, options);

    return response;
  } catch (error) {
    const errorMessage = getRequestErrorMessage(error);

    throw new Error(errorMessage);
  }
};
