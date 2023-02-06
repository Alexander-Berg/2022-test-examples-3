(function() {
    if (!Ya.Search || !Ya.Search.Suggest) return;

    Ya.define('mini-suggest-hermione-stub-touch-phone', [], function() {
        function overrides() {
            MBEM.decl({ block: 'mini-suggest', modName: 'search', modVal: 'yes' }, {
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
                        var urlParams = new URLSearchParams(window.location.search);
                        var suggestState = urlParams.get('save_suggest_state');

                        this._appendHiddenInput('save_suggest_state', this._suggestState || suggestState + 1);
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
            Ya.Search.Suggest.overrides = Ya.Search.Suggest.overrides || [];
            Ya.Search.Suggest.overrides.push(overrides);
        }
    });
})();
