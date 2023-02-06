/**
 * Получить reqid с открытой страницы
 *
 * @returns {Promise<String>}
 */
module.exports = function yaGetReqId() {
    return this.execute(function() { return __GLOBAL_CONTEXT__.reqid }).then(ret => ret.value);
};
