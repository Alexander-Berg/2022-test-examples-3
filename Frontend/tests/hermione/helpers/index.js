module.exports = {
    /**
     * Декорирует xhr.open и fetch, добавляя кастомный query-параметр в URL.
     * Позволяет избежать кэширования ответа в клементе
     *
     * @param {String} hash значение параметра _hash
     */
    setAjaxHash: hash => {
        const setHash = (url, method, hash) => {
            if (method !== 'GET') {
                return url;
            }

            const parsed = new URL(location.origin + url);
            parsed.searchParams.set('_hash', hash);

            return parsed.href;
        };

        const _xhrOpen = window.XMLHttpRequest.prototype.open;
        window.XMLHttpRequest.prototype.open = function() {
            const args = Array.from(arguments);
            args[1] = setHash(args[1], args[0], hash);

            return _xhrOpen.apply(this, args);
        };

        const _fetch = window.fetch;
        window.fetch = function(url, options) {
            url = setHash(url, (options && options.method) || 'GET', hash);

            return _fetch(url, options);
        };
    },
    /**
     * Мок ответа для m-suggest
     * @param {Array} response - массив объектов для m-suggest в стандартном формате ответа с сервера
     */
    mockSuggest: function(response) {
        window.BEM.decl('m-suggest', {
            _sendRequest: function() {
                this.setState({
                    result: response,
                });
            },
        });
    },

    /**
     * Чтобы проверить кликабельность ссылки с target: _blank
     * Отменяем открытие ссылки в новом окне
     * @param {String} selector
     */
    disableTargetBlank: function(selector) {
        const link = document.querySelector(selector);

        link.target = '_self';
    },

    /**
     * Чистит localstorage
     * @param {String} key
     */
    clearLocalStorage: function(key) {
        window.localStorage.removeItem(key);
    },
};
