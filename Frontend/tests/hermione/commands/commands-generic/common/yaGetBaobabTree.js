const validator = counters => {
    assert.isDefined(
        counters.server && counters.server.tree,
        'Не удалось получить баобаб-дерево из серверных счётчиков'
    );
};

/**
 * Получить из blockstat-лога дерево баобаба
 *
 * @param {String} [reqid] - reqid
 *
 * @returns {Promise<Object>}
 */
module.exports = function yaGetBaobabTree(reqid) {
    return this
        .yaGetCounters(validator, reqid)
        .then(counters => counters.server.tree);
};
