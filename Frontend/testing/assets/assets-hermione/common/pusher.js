/**
 * Мок блока pusher для тестов
 *
 * Хранит статусы подписки локально; не ходит в бэкенд; не спрашивает права.
 * См. оригинал: https://nda.ya.ru/t/Xg9iCgbV3VyBpa.
 */
BEM.DOM.decl('pusher', {}, {
    /**
     * Получение прав
     *
     * @returns {String} granted | denied | default
     */
    getPermission: function() {
        return 'granted';
    },

    /**
     * Проверка статусов подписки
     *
     * @param {PusherTag|PusherTag[]} tags - идентификаторы подписок, которые проверяем
     *
     * @returns {Promise<Boolean>}
     */
    check: function(tags) {
        for (var i = 0; i < tags.length; i++) {
            if (!this.subscriptions[tags[i].value]) {
                return Promise.resolve(false);
            }
        }

        return Promise.resolve(true);
    },

    /**
     * Отписка
     *
     * @param {PusherTag|PusherTag[]} tags - идентификаторы подписок, значения которых устанавливаем
     *
     * @returns {Promise<Boolean>}
     */
    subscribe: function(tags) {
        for (var i = 0; i < tags.length; i++) {
            this.subscriptions[tags[i].value] = true;
        }

        return Promise.resolve(true);
    },

    /**
     * Отписка
     *
     * @param {PusherTag|PusherTag[]} tags - идентификаторы подписок, значения которых устанавливаем
     *
     * @returns {Promise<Boolean>}
     */
    unsubscribe: function(tags) {
        for (var i = 0; i < tags.length; i++) {
            this.subscriptions[tags[i].value] = false;
        }

        return Promise.resolve(false);
    },

    subscriptions: {}, // тут будем хранить статусы подписки
    supported: true // указываем, что подписка поддерживается независимо ни от чего
});
