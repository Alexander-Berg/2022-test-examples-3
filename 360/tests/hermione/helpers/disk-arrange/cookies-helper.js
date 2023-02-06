module.exports = {
    /**
     * Собирает из заголовков Set-Cookie пары key=value и возвращает их в качестве объекта.
     *
     * @param {Object} headers - объект с заголовками, пришедшими с сервера
     * @returns {Object}
     */
    readCookies(headers) {
        let cookies = headers['set-cookie'];
        const result = {};

        if (cookies) {
            if (!Array.isArray(cookies)) {
                cookies = [cookies];
            }

            cookies.forEach((cookie) => {
                const pair = cookie.split(';')[0].split('=');
                if (pair.length === 2) {
                    result[pair[0]] = pair[1];
                }
            });
        }

        return result;
    },
    /**
     * Собирает из объекта строковое представление заголовка Cookie и записывает его в объект
     *
     * @param {Object} cookies - объект с куками
     * @param {Object} [headers] - объект с заголовками, в который необходимо записать заголовок с куками;
     *                             если не передан, то будет возвращен новый.
     * @returns {Object}
     */
    writeCookies(cookies, headers = {}) {
        headers.Cookie = Object.keys(cookies).map((key) => `${key}=${cookies[key]}`).join('; ');
        return headers;
    }
};
