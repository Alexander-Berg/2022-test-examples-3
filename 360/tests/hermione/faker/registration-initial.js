/**
 * @typedef {Object} AlmostService
 * @property {number} id
 * @property {string|null} requiredAcquirer
 */

/**
 * @typedef {Object} AlmostCategory
 * @property {number} id
 * @property {string|null} requiredAcquirer
 */

/**
 * @typedef {Object} InitialData
 * @property {string} inn
 * @property {number} service
 * @property {number[]} categories
 * @property {boolean} requireOnline
 */

/**
 * @function
 * @param {AlmostService} service
 * @returns {boolean}
 */
const filterServiceWithoutAcquirer = service => {
  return service.requiredAcquirer === null;
};

/**
 * @function
 * @param {AlmostCategory} category
 * @returns {boolean}
 */
const filterCategoryWithoutAcquirer = category => {
  return category.requiredAcquirer === null;
};

/**
 * @function
 * @param {AlmostService} service
 * @returns {number}
 */
const getServiceId = service => {
  return service.id;
};

/**
 * @function
 * @param {AlmostCategory} category
 * @returns {number}
 */
const getCategoryId = category => {
  return category.id;
};

/**
 * @function
 * @param {Object} options
 * @param {AlmostService[]} options.services
 * @param {AlmostCategory[]} options.categories
 * @param {boolean} options.withIPType
 * @returns {() => InitialData}
 */
const makeInitialDataCreator = ({ services, categories, withIPType }) => () => {
  const servicesWithoutAcquirer = services
    .filter(filterServiceWithoutAcquirer)
    .slice(0, 1);
  const categoriesWithoutAcquirer = categories
    .filter(filterCategoryWithoutAcquirer)
    .slice(0, 10);

  if (servicesWithoutAcquirer.length === 0) {
    throw new Error("Couldn't find any services without an acquirer");
  }

  if (categoriesWithoutAcquirer.length === 0) {
    throw new Error("Couldn't find any categories without an acquirer");
  }

  return {
    inn: withIPType ? '510105936358' : '5031076070',
    service: servicesWithoutAcquirer.map(getServiceId)[0],
    categories: categoriesWithoutAcquirer.map(getCategoryId),
    requireOnline: true
  };
};

module.exports = {
  makeInitialDataCreator
};
