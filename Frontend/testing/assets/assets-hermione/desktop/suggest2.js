(function() {
    BEM.decl('suggest2-provider', {
        _extendParamsData: function() {
            this.__base.apply(this, arguments);

            this.params.data.originalUrl = this.originalUrl.replace('yandex.ru/suggest/', 'suggest.yandex.ru/');

            return this.params;
        }
    });

    Ya.define('mini-suggest-hermione-stub-desktop', [], function() {
        function overrides() {
            MBEM.decl('mini-suggest', {
                _onResponse: function(text, data, opts) {
                    BEM.channel('main-suggest').trigger('ajaxSuccess', opts.url);
                    this.__base.apply(this, arguments);
                },

                /**
                 * Получить строку запроса для отправки счётчиков.
                 *
                 * Не используем this.params.counter.url - он может содержать production hostname "//yandex.ru",
                 * и счётчики уйдут не туда, куда надо.
                 *
                 * Для проверки счётчиков саджеста в тестах hermione в запрос добавлены параметры
                 * 'reqid=' и 'vars=suggest'.
                 *
                 * @returns {String}
                 * @private
                 */
                _getCounterUrl: function() {
                    return '/clck/jclck' + '/' +
                        ['reqid=' + BEM.blocks['i-global'].param('reqid'), 'vars=suggest']
                            .concat(this._getCounterUrlParams())
                            .join('/')
                            .replace(/\/+/g, '/');
                },

                _getMainUrlParams: function() {
                    var params = this.__base.apply(this, arguments);

                    params.svg = 0;

                    return params;
                },

                _attachReqID: function() {
                    this.__base.apply(this, arguments);

                    if (location.href.indexOf('&save_suggest_reqid') > 0) {
                        this._appendHiddenInput('suggest_reqid_saved', this._reqID);
                        this._appendHiddenInput('save_suggest_reqid', 1);
                    }

                    if (location.href.indexOf('&save_suggest_state') > 0) {
                        this._appendHiddenInput('save_suggest_state', this._suggestState || BEM.blocks.uri
                            .parse(window.location.href)
                            .getParam('save_suggest_state') + 1);
                    }

                    if (location.href.indexOf('&save_suggest_reqid_cookie') > 0) {
                        var cookie = window.jQuery.cookie;

                        this._appendHiddenInput('save_suggest_reqid_cookie', 1);
                        cookie.set('suggest_reqid_saved', cookie.get('suggest_reqid'));
                    }
                }
            });
        }

        // Асинхронная инициализация доопределений на уровне сервиса
        if (window.MBEM) {
            overrides();
        } else {
            window.Ya = window.Ya || {};
            Ya.Search = Ya.Search || {};
            Ya.Search.Suggest = Ya.Search.Suggest || {};
            Ya.Search.Suggest.overrides = Ya.Search.Suggest.overrides || [];
            Ya.Search.Suggest.overrides.push(overrides);
        }
    });
})();
