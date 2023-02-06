/**
 * Позволяет залогиниться под пользователем с соответствующей кукой.
 * Нужно в основном для записи дампов
 */

module.exports = function yaLogin(sessionId) {
    this
        .url('https://www.yandex.ru')
        .setCookie({
            name: 'Session_id',
            value: sessionId,
            domain: '.yandex.ru',
        });
};
