if (window.location.search.indexOf('hermione_yanalytics=stub') > 0) {
    modules.decl('ganalytics', ['i-bem-dom'], function(provide, bemDom) {
        // Возможность заменить библиотеку yanalytics (https://github.yandex-team.ru/lp-constructor/yanalytics/)
        // на стаб для тестирования при передаче параметра `&exp_flags=hermione_yanalytics=stub`.
        var Ganalytics = bemDom.declBlock(this.name, {}, {
            SOURCE: '/static/turbo/hermione/stubs/yanalytics.js',
        });

        provide(Ganalytics);
    });
}
