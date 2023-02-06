(function() {
    // Возможность отключить загрузку рекламы под параметром &hermione_advert=stub.
    //
    // NOTE (gfranco@): мне абсолютно непонятна природа этого кода, но т.к. это мешает
    // стабить рекламу в реактовом блоке (если Ya.areAdsStubbed === true, то рекламные
    // скрипты вообще не загружаются), а автор недоступен - в качестве временного решения
    // добавляю такое условие.
    if (window.location.search.indexOf('hermione_advert=stub') > 0 && window.location.search.indexOf('load-react-advert-script') === -1) {
        /*
         * Переопределение для реактовых компонентов:
         * Так как мы не можем получать параметр стаба просто, модифицируя прототип,
         * мы подписываем стаб в глобальную переменную.
         */
        window.Ya.areAdsStubbed = true;
    }
})();

modules.define('advert__item', function(provide, AdvertItem) {
    // Возможность отключить загрузку рекламы под параметром &hermione_advert=stub.
    if (window.location.search.indexOf('hermione_advert=stub') > 0) {
        /*
         * Переопределяем методы прототипа, т.к. react advert-stick вызывает onInit
         * До того, как загрузился модуль бандла. Таким образом на десктопах происходит следующий порядок:
         *
         * Таким образом на десктопах происходит следующий порядок:
         * 1. declBlock обычного блока advert
         * 2. вызов onInit из react блока AdvertSticks
         * 3. declBlock advert из стаба
         * 4. событие, которое окончательно инитит advert.
         *
         * Подменяя методы прототипа, мы можем изменить поведение блока
         */
        var superLoadAdvertScript = AdvertItem.prototype._loadAdvertScript;

        AdvertItem.prototype._loadAdvertScript = function() {
            this._script = this.params.advertScriptSrc =
                '/static/turbo/hermione/stubs/advert.js';

            superLoadAdvertScript.call(this);
        };
    }

    provide(AdvertItem);
});
