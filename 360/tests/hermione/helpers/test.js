const path = require('path');
const tr12n = require('transliteration');

/**
 * @typedef {Object} TestMeta
 * @property {string} id
 * @property {string} title
 * @property {string} file
 * @property {string} browserId
 */

/**
 * @function
 * @param {any} test
 * @returns {string}
 */
const getTestScreenshotsDir = test => {
  return path.join(
    path.dirname(test.file),
    'screens',
    [tr12n.slugify(test.title), test.id].join('-'),
    test.browserId
  );
};

/**
 * @function
 * @param {any} test
 * @returns {TestMeta}
 */
const getTestMeta = test => {
  return {
    id: String(test.id),
    title: String(test.title),
    file: String(test.file),
    browserId: String(test.browserId)
  };
};

module.exports = {
  getTestScreenshotsDir,
  getTestMeta
};
