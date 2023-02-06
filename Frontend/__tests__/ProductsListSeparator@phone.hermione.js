specs({
    feature: 'products-list-separator',
}, () => {
    const srcrwrCgi = '&srcrwr=SEARCH_SAAS_LISTINGS:SAAS_ANSWERS';

    hermione.only.notIn('safari13');
    it('Подскроливание к указанной странице', function() {
        return this.browser
            .url(`/turbo?text=logomebel.ru%2Fyandexturbocatalog%2F&page=2${srcrwrCgi}`)
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaAssertViewportView('autoscroll');
    });

    hermione.only.notIn('safari13');
    it('Не должен подскроливать к нулевой странице', function() {
        return this.browser
            .url(`/turbo?text=logomebel.ru/yandexturbocatalog/&page=0${srcrwrCgi}`)
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            // Задержка на тротлинг, чтобы убедиться, что подскрол не сработал позднее.
            .pause(1100)
            .execute(function() {
                return window.scrollY;
            }).then(function({ value }) {
                return assert.strictEqual(value, 0);
            });
    });

    hermione.only.notIn('safari13');
    it('Изменение номера страницы по мере скрола вверх', function() {
        return this.browser
            .url(`/turbo?text=logomebel.ru%2Fyandexturbocatalog%2F&page=2${srcrwrCgi}`)
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            // Ждём запрос за предыдущими товарами и завершения удержания первоначальной позиции (Autoload2#keepInitialScroll).
            .pause(1500)
            // Подскроливаем чуть выше, чтобы последний ряд заглушек товаров второй страницы был полностью виден,
            // чтобы потом выполнилась проверка на видимость загруженных товаров.
            .yaScrollPageBy(-600)
            .yaWaitForVisible('a[data-page="1"]', 3000, 'Не загрузилась предыдущая страница товаров')
            // Ждём отработки троттла на изменение адреса страницы.
            .yaWaitUntil('Номер страницы не поменялся', function() {
                return this.getUrl().then(function(url) {
                    return url.indexOf('&page=1') > -1;
                });
            }, 1500, 250);
    });

    hermione.only.notIn('safari13');
    it('Изменение номера страницы по мере скрола вниз', function() {
        return this.browser
            .url(`/turbo?text=logomebel.ru%2Fyandexturbocatalog%2F&page=2${srcrwrCgi}`)
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            // Ждём завершения удержания первоначальной позиции (Autoload2#keepInitialScroll).
            .pause(1000)

            // Подскроливаем чуть выше, чтобы последний ряд заглушек товаров второй страницы был полностью виден,
            // чтобы потом выполнилась проверка на видимость загруженных товаров.
            .yaScrollPageBy(-300)
            // Дожидаемся загрузки предыдущей страницы, потому что иначе скрол страницы в тесте начнёт
            // колбасить после загрузки предыдущей страницы товаров.
            .yaWaitForVisible('a[data-page="1"]', 3000, 'Не загрузилась предыдущая страница товаров')

            .yaScrollPageToBottom()
            .yaWaitForVisible('a[data-page="3"]', 10000, 'Не загрузилась следующая страница товаров')
            // Ждём отработки троттла на изменение адреса страницы.
            .yaWaitUntil('Номер страницы не поменялся', function() {
                return this.getUrl().then(function(url) {
                    return url.indexOf('&page=3') > -1;
                });
            }, 1500, 250);
    });
});
