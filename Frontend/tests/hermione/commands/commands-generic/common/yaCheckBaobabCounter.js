const _ = require('lodash');
const helpers = require('../../utils/baobab');

/**
 * Проверить серверный (blockstat) счетчик баобаба.
 * Поиск ноды в баобаб-дереве с путем expected.path и атрибутами expected.attrs
 *
 * @param {String|Function} action - Действие, которое необходимо выполнить для срабатывания счётчиков или счётчика
 *                                 Если action - строка, будет выполнен click(action)
 * @param {Object} expected - счётчики, которые должны сработать
 * @param {Object} options - опции
 *
 * @returns {Promise}
 */
module.exports = function yaCheckBaobabCounter(action, expected, options = {}) {
    if (typeof action === 'string') {
        const selector = action;
        action = () => this.click(selector);
    }

    if (typeof action !== 'function') {
        throw new Error('action должен быть типа Function');
    }

    let reqid;

    return Promise.resolve()
        .then(() => reqid = options.reqid || this.yaGetReqId())
        .then(action)
        .then(() => this.yaGetBaobabTree(reqid))
        .then(tree => {
            const nodes = helpers.query(expected.path, tree.tree, expected.attrs);

            if (!nodes.length) {
                let message = `В дереве Баобаба не найден узел с путем ${expected.path}`;

                if (expected.attrs) {
                    message += ` и атрибутами ${JSON.stringify(expected.attrs)}`;
                }

                throw new Error(message);
            }

            const validator = counters => {
                const client = counters.client;
                let found = client.find(item => {
                    if (_.get(item, 'vars.-baobab-event-json')) {
                        const data = JSON.parse(decodeURIComponent(item.vars['-baobab-event-json']))[0];
                        const node = nodes.find(i => i.id === data.id);

                        if (node) {
                            return _.isEqual(
                                _.omit(data, ['cts', 'mc']),
                                Object.assign(
                                    { id: node.id, event: 'click' },
                                    _.omit(expected, ['path', 'attrs'])
                                )
                            );
                        }
                    } else if (item.bu) {
                        return Boolean(nodes.find(i => i.id === item.bu));
                    }

                    return null;
                });

                assert.isDefined(found, `В логах не найдена запись для ${JSON.stringify(expected)}`);
            };

            return this.yaGetCounters(validator, reqid);
        });
};
