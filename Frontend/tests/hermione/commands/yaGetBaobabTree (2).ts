const validator = (counters) => {
    assert.isDefined(
        counters.server && counters.server.tree,
        'Не удалось получить баобаб-дерево из серверных счётчиков',
    );
};

/**
 * Получить из blockstat-лога дерево баобаба
 *
 * @param {String} [reqid] - reqid
 *
 * @returns {Promise<Object>}
 */
module.exports = async function yaGetBaobabTree(reqid: string) {
    const counters = await this.yaGetCounters(validator, reqid);
    return counters.server.tree;
};
