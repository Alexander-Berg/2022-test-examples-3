/**
 * Ставит куку по которой добавляется GET-параметр ко всем запросам в бэк.
 * Ставить перед тем как происходит модифицирующий запрос для того чтобы
 * запросы с новыми данными не перезаписывали существующие.
 * @param {String} hash
 * @returns {Promise}
 */
module.exports = function(hash) {
    return this.setCookie({
        name: '__hash__',
        value: hash,
    })
        .waitUntil(async() => {
            const cookie = await this.getCookie('__hash__');

            return cookie.value === hash;
        }, 1000, `Не удалось поставить куку __hash__: ${hash}`);
};
