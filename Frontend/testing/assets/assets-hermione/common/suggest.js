(function(original) {
    window.hermione.alertCounter = 0;
    window.alert = function() {
        window.hermione.alertCounter++;
        original.apply(this, arguments);
    };
})(window.alert);

(function() {
    BEM.decl('suggest2-input', {
        bindEvents: function(events, context) {
            Object.keys(events).forEach(function(event) {
                if (this._bindedEventsNames.indexOf(event) === -1) {
                    this._bindedEventsNames.push(event);
                }

                // Debounce для события "change", чтобы сделать работу саджеста более
                // предсказуемой, уменьшив тем самым кол-во дампов саджеста.
                var fn = event === 'change' ?
                    $.debounce(events[event].bind(context || this), 200, false) :
                    events[event].bind(context || this);

                this._eventsFilter(event).on(event, fn);
            }, this);
            return this;
        }
    });

    BEM.decl('suggest2-provider', {
        _extendParamsData: function() {
            this.__base.apply(this, arguments);

            // Перенаправляем запрос к саджесту в локальную ручку.
            if (!this.originalUrl) this.originalUrl = this.params.url;

            // Увеличиваем таймаут к локальной ручке саджеста
            this.params.timeout = 10000;
            // Добавляем дополнительные параметры необходимые для дампа
            this.params.data = $.extend(this.params.data || {}, {
                originalUrl: this.originalUrl,
                tpid: window.hermione.meta.tpid
            });
            this.params.url = '/stubs/suggest';

            return this.params;
        }
    });

    BEM.DOM.decl('suggest2-counter', {
        /**
         * Получить строку запроса для отправки счётчиков.
         *
         * Для проверки счётчиков саджеста в тестах hermione в запрос добавлены параметры 'reqid=' и 'vars=suggest'.
         *
         * @returns {String}
         */
        getUrl: function() {
            return ['/', this.params.path, 'reqid=' + BEM.blocks['i-global'].param('reqid'), 'vars=suggest']
                .concat(this._getUrlParams())
                .join('/')
                .replace(/(\/+)/g, '/');
        }
    });
})();
