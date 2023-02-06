/**
 * @function
 * @param {number} milliseconds
 * @returns {Promise<void>}
 */
const wait = milliseconds => {
  return new Promise(resolve => {
    return setTimeout(resolve, milliseconds);
  });
};

module.exports = {
  wait
};
