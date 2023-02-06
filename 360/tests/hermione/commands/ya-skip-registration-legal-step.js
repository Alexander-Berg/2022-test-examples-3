const { makeLegalDataCreator } = require('../faker/registration-legal');

/**
 * @typedef {import('../faker/registration-legal').LegalData} LegalData
 */

/**
 * @function
 * @param {any} browser
 * @param {LegalData} initialData
 * @returns {Promise<any>}
 */
const createMerchant = (browser, legalData) => {
  return browser.yaApiRequest('api/merchant', {
    method: 'post',
    json: legalData
  });
};

/**
 * @function
 * @param {Object} options
 * @param {boolean} options.withIPType
 * @param {boolean} [options.withConcactPerson]
 * @param {boolean} [options.withPostAddress]
 * @returns {Promise<any>}
 */
module.exports = async function yaSkipRegistrationLegalStep({
  withIPType,
  withConcactPerson = false,
  withPostAddress = false
}) {
  const { login } = await this.yaGetUser();
  const legalData = await this.yaUseFaker(
    makeLegalDataCreator({
      withIPType,
      withConcactPerson,
      withPostAddress,
      userLogin: login
    })
  );

  return createMerchant(this, legalData);
};
