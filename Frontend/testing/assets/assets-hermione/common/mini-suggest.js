(function() {
    if (!Ya.Search || !Ya.Search.Suggest) return;

    Ya.define('mini-suggest-hermione-stub-common', [], function() {
        function overrides() {
            var testRunId;

            // fallback на проброс testRunId в query параметры
            // SERP-154057
            if (document.cookie.indexOf('testRunId') === -1) {
                testRunId = new URLSearchParams(window.location.search).get('testRunId');
            }

            var stubHandle = '/stubs/suggest?',
                stubUrl = function(url) {
                    if (url.indexOf(stubHandle) === 0) {
                        return url;
                    }

                    var parts = url.split('?'),
                        host = parts.shift(),
                        query = parts.join('?');

                    return stubHandle +
                        'tpid=' + window.hermione.meta.tpid +
                        (testRunId ? '&testRunId=' + testRunId : '') +
                        '&originalUrl=' + host + '&' + query;
                };

            MBEM.decl('mini-suggest', {
                _onResponse: function(text, data, opts) {
                    BEM.channel('main-suggest').trigger('ajaxSuccess', opts.url);

                    this.__base.apply(this, arguments);
                }
            });

            MBEM.decl({ block: 'mini-suggest', modName: 'request' }, {
                /**
                 * Формируем урл для запроса подсказок
                 *
                 * @param {String} text - текст запроса
                 *
                 * @returns {String}
                 * @protected
                 */
                _prepareRequestUrl: function(text) {
                    var url = this.__base.apply(this, arguments);

                    if (location.href.indexOf('&save_suggest_state') > 0) {
                        this._suggestState = this._suggestState ||
                            BEM.blocks.uri.parse(window.location.href).getParam('save_suggest_state');

                        url = MBEM.appendUrlParams(url, { suggestState: String(this._suggestState++) });
                    }

                    this._preparedUrl = stubUrl(url);

                    return this._preparedUrl;
                },

                /**
                 * Делаем запрос за подсказками
                 *
                 * @param {String} text - текст запроса
                 * @param {Function} successCallback - callback при успешном выполнении запроса
                 * @param {Function} errorCallback - callback при неуспешном выполнении запроса
                 *
                 * @protected
                 *
                 * @fires MiniSuggest#request
                 */
                _request: function(text) {
                    this.__base.apply(this, arguments);

                    BEM.channel('main-suggest').trigger('ajaxSend', this._preparedUrl);
                }
            });

            MBEM.decl({ block: 'mini-suggest', modName: 're-request', modVal: 'yes' }, {
                _reRequest: function() {
                    this.__base.apply(this, arguments);

                    BEM.channel('main-suggest').trigger('ajaxSend', this._preparedReRequestUrl);
                },

                _prepareReRequestUrl: function() {
                    this._preparedReRequestUrl = stubUrl(this.__base.apply(this, arguments));

                    return this._preparedReRequestUrl;
                }
            });

            MBEM.decl({ block: 'mini-suggest', modName: 'personal', modVal: 'yes' }, {
                _onDeleteClick: function() {
                    this.params.deleteUrl = stubUrl(this.params.deleteUrl);

                    this.__base.apply(this, arguments);

                    // передаю базовый урл, так как результирующий формируется
                    // в методе и доступа до него в данной реализации нет
                    BEM.channel('main-suggest').trigger('ajaxSend', this.params.deleteUrl);
                }
            });

            MBEM.decl({ block: 'mini-suggest', modName: 'entity-search', modVal: 'yes' }, {
                _getEntityUrl: function() {
                    return this.__base.apply(this, arguments) + '&tpid=' + window.hermione.meta.tpid;
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
