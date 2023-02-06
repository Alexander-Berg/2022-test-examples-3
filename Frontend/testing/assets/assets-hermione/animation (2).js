// Так как, скрипты у нас грузяться через defer, то дождаться их можно только так
window.addEventListener('load', function() {
    modules.define('smooth-scroll', ['inherit'], function(provide, inherit, SmoothScroll) {
        var SmoothScrollNoDuration = inherit(SmoothScroll, {
            /**
             * Для smooth-scrolling к заданному элементу
             *
             * @param {HTMLElement} target - элементк к которому нужно проскролить
             * @param {Object} options - настройки скрола
             * @param {number} [options.duration=100] - время анимации
             * @param {number} [options.infelicity=0] - добавочное значение (e.g. проскролить к элементу + 8px)
             * @param {Function} [options.callback] - функция которую нужно вызвать после выполнения анимации
             */
            scrollTo: function(target, options) {
                options || (options = {});
                options.duration = 0;

                this.__base.apply(this, target, options);
            },
        });

        provide(SmoothScrollNoDuration);
    });

    if (window && window.Ya && window.Ya.getStore) {
        // Отключение JS-анимаций для контентного турбо.
        window.Ya.getStore().dispatch({ type: 'SETTINGS/DISABLE_ANIMATIONS' });

        // Отключение JS-анимаций для екомерса-тапов.
        window.Ya.getStore().dispatch({ type: '@@meta/DISABLE_ANIMATIONS' });
    }
});
