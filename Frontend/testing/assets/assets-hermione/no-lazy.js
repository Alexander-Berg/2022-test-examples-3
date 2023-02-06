/**
 * Пробуем "выключить" всю ленивость
*/

if (document.readyState === 'complete' || document.readyState === 'loaded') {
    hermioneSetFakeLazy();
} else {
    document.addEventListener('DOMContentLoaded', hermioneSetFakeLazy);
}

function hermioneSetFakeLazy() {
    if (window.location.search.indexOf('hermione_no-lazy=1') > 0) {
        // Подменяем для React
        function FakeIntersectionObserver(cb, options) {
            this.cb = cb;
            this.options = options;
        }

        FakeIntersectionObserver.prototype.disconnect = function() {};
        FakeIntersectionObserver.prototype.observe = function(node) {
            var cb = this.cb;

            setTimeout(function() {
                cb([{
                    isIntersecting: true,
                    intersectionRatio: 1,
                    target: node,
                }]);
            }, 0);
        };

        window.IntersectionObserver = FakeIntersectionObserver;

        // Тригирим enterView сразу
        modules.decl('viewport-watcher', ['i-bem-dom'], function(provide, bemDom) {
            var ViewportWatcher = bemDom.declBlock(this.name, {
                onSetMod: {
                    js: {
                        inited: function() {
                            this.enterView();
                        },
                    },
                },
            });

            provide(ViewportWatcher);
        });
    }
}
