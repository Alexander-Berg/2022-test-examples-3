/**
 * Включает экспериментальные флаги при проведении теста
 *
 * @param flags {string[] | string}
 * @returns {Promise<void>}
 */
module.exports = async function yaEnableExpFlags(flags) {
    await this.setMeta('exp-flags', flags);
};
