const { makeInitialDataCreator } = require('../faker/registration-initial');

/**
 * @typedef {import('../faker/registration-initial').InitialData} InitialData
 */

/**
 * @function
 * @param {any} browser
 * @returns {Promise<any>}
 */
const getAvailableServices = browser => {
  return browser.yaApiRequest('api/service');
};

/**
 * @function
 * @param {any} browser
 * @returns {Promise<any>}
 */
const getAvailableCategories = browser => {
  return browser.yaApiRequest('api/category');
};

/**
 * @function
 * @param {any} browser
 * @param {InitialData} initialData
 * @returns {Promise<any>}
 */
const preregisterMerchant = (browser, initialData) => {
  return browser.yaApiRequest('api/merchant/preregister', {
    method: 'post',
    json: initialData
  });
};

/**
 * @function
 * @param {Object} options
 * @param {boolean} options.withIPType
 * @returns {Promise<any>}
 */
module.exports = async function yaSkipRegistrationInitialStep({ withIPType }) {
  const [services, categories] = await Promise.all([
    getAvailableServices(this),
    getAvailableCategories(this)
  ]);
  const initialData = await this.yaUseFaker(
    makeInitialDataCreator({
      withIPType,
      services,
      categories
    })
  );

  return preregisterMerchant(this, initialData);
};
