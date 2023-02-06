// используется только при записи дампов, не работает в beforeEach
module.exports = function yaLogin() {
    return this
        .url('https://www.yandex.ru')
        .setCookie({
            name: 'Session_id',
            value: process.env.SESSION_ID || '',
            domain: '.yandex.ru',
        });
};
