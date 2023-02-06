const COOKIE_NAME = 'yexp';

/**
 * Проверяет, что кука yexp существует/не существует
 *
 * @param {Boolean} isExistsCookie - Должен ли существовать cookie yexp
 *
 * @returns {Promise}
 */
module.exports = function(isExistsCookie = true) {
    return this
        .getCookie()
        .then(cookie => {
            const yexp = cookie.filter(item => item.name === COOKIE_NAME);

            if (isExistsCookie) {
                assert.isTrue(yexp.length > 0, `cookie с именем ${COOKIE_NAME} не установлено`);
            } else {
                assert.isTrue(yexp.length === 0, `cookie с именем ${COOKIE_NAME} не должно существовать`);
            }
        });
};
