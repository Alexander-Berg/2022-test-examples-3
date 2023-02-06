(function() {
    const autoloadDisabled = window.location.search.indexOf('hermione_autoload=disable') > 0;
    if (autoloadDisabled) {
        window.__DISABLE_AUTOLOAD__ = true;
    }

    modules.decl('autoload', ['i-bem-dom'], function(provide, bemDom) {
        var Autoload = bemDom.declBlock(this.name, {
            onSetMod: {
                js: {
                    inited: function() {
                        this.__base.call(this, arguments);

                        // Чтобы можно было переключить autoload в offline/online для теста
                        window.__autoload_offline = this._offlineCallback;
                        window.__autoload_online = this._onlineCallback;
                    },
                },
            },
            // Переопределяем обработчик на скролл, чтобы ничего не делать.
            _enterView: function() {
            // Возможность отключить дозагрузку страниц под параметром &hermione_autoload=disable.
                if (autoloadDisabled) {
                    return;
                }

                this.__base();
            },
        });

        provide(Autoload);
    });
})();
