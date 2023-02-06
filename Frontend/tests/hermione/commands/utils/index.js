const _ = require('lodash');

module.exports = {
    firstOrDefault: res => Array.isArray(res) ? res[0] : res || '',

    /**
     * Выполняет проверки последовательно
     *
     * Example
     *
     * it('Набор проверок', function() {
     *  return sequence(this.browser, [this.PO.title(), this.PO.subtitle()].map(selector => function() {
     *      return this.yaShouldBeVisible(selector);
     * }));
     *
     * @param {Object} ctx - контекст вызова проверки (обычно this в it-e или this.browser)
     * @param {Function[]} actions - массив проверок
     *
     * @returns {Promise}
     */
    sequence: (ctx, actions) => actions.reduce(
        (promise, action) => promise.then(action.bind(ctx)), Promise.resolve()
    ),

    /**
     * Провалидировать объект на наличие обязательных и отсутствие лишних полей
     *
     * @param {Object} data - объект для проверки
     * @param {Object} constraints - список обязательных полей
     * @param {String[]} constraints.required - список обязательных полей*
     * @param {String[]} constraints.allowed - список всех возможных полей
     * @param {String} message - сообщение для ошибки
     */
    validateFields(data, constraints, message) {
        if (typeof data !== 'object') {
            throw new Error(`${message}: ${JSON.stringify(data)} должен быть объектом`);
        }

        const fields = Object.keys(data);
        const required = constraints.required || [];
        const intersection = _.intersection(required, fields);

        if (intersection.length !== required.length) {
            throw new Error(
                `${message}: объект ${JSON.stringify(data)} не содержит обязательных полей ${JSON.stringify(required)}`
            );
        }

        const allowed = _.uniq([].concat(required, constraints.allowed || []));
        const difference = _.difference(fields, allowed);

        if (difference.length) {
            throw new Error(
                `${message}: объект ${JSON.stringify(data)} содержит лишние поля: "${JSON.stringify(difference)}"`
            );
        }
    }
};
