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
};
