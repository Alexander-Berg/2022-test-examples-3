const helpers = require('../../utils/baobab');

/**
 * Проверить серверный (blockstat) счетчик баобаба.
 * Поиск ноды в баобаб-дереве с путем expected.path и атрибутами expected.attrs
 *
 * @param {BaobabNodeData} expected - счётчики, которые должны сработать
 * @param {Object} [options] - опции
 * @param {String} [options.reqid] - reqid
 *
 * @returns {Promise}
 */
module.exports = function yaCheckBaobabServerCounter(expected, options = {}) {
    return this
        .yaGetBaobabTree(options.reqid)
        .then(tree => {
            const nodes = helpers.query(expected.path, tree.tree, expected.attrs);

            if (!nodes.length) {
                let message = `В дереве Баобаба не найден узел с путем ${expected.path}`;

                if (expected.attrs) {
                    message += ` и атрибутами ${JSON.stringify(expected.attrs)}`;
                }

                throw new Error(message);
            }
        });
};
