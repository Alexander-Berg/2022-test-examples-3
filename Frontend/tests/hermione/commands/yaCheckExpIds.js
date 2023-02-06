const COOKIE_NAME = 'yexp';

/**
 * Проверяет, что данные эксперименты удовлетворяют экспериментам в куке yexp
 *
 * @param {Array} expIds - Id экспериментов
 * @param {Boolean} isStuck - Проверять на залипание или отлипание
* @param {Boolean} isExistsCookie - Должен ли существовать cookie yexp
 *
 * @returns {Promise}
 */
module.exports = function(expIds, isStuck = true, isExistsCookie = true) {
    return this
        .getCookie()
        .then(cookie => {
            if (!isExistsCookie) {
                return;
            }

            const yexp = cookie.filter(item => item.name === COOKIE_NAME);
            const cookieExpIds = yexp[0].value
                .split('.')[0]
                .slice(1)
                .split('_')
                .map(Number);

            const hasExpIds = expIds.every(expId => isStuck ?
                cookieExpIds.includes(expId) :
                !cookieExpIds.includes(expId));

            assert.isTrue(hasExpIds, `Не залипнут во всех экспериментах: ${expIds}`);
        });
};
