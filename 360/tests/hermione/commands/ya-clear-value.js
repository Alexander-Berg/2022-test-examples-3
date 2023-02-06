/**
 * @param {string} selector
 * @returns {Promise<any>}
 */
module.exports = async function yaClearValue(selector) {
  const value = await this.getValue(selector);
  const backspaces = '\uE003'.repeat(value.length);

  return this.setValue(selector, backspaces);
};
