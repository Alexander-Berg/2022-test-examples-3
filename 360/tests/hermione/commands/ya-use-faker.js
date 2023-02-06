const faker = require('faker/locale/ru');

/**
 * @typedef {import('faker')} Faker
 */

/**
 * Set a random seed for faker for each test.
 *
 * @function
 * @param {(faker: Faker) => any} creator
 * @returns {any}
 */
module.exports = async function yaUseFaker(creator) {
  const test = await this.yaGetTest();
  const seed = Math.max(test.title.length, 1);

  faker.seed(seed);

  return creator(faker);
};
