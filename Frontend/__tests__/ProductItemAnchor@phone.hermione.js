const { URL } = require('url');

specs({
    feature: 'product-item-anchor',
}, () => {
    hermione.only.notIn('safari13');
    it('Подскроливание к указанному товару', function() {
        return this.browser
            .url('/turbo?text=logomebel.ru%2Fyandexturbocatalog%2F&srcrwr=SEARCH_SAAS_LISTINGS:SAAS_ANSWERS&page=4#p6039')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaAssertViewportView('autoscroll');
    });

    hermione.only.notIn('safari13');
    it('Подскроливание к указанному товару в оверлее', function() {
        return this.browser
            .yaOpenInIframe('?srcrwr=SEARCH_SAAS_LISTINGS:SAAS_ANSWERS&text=logomebel.ru/yandexturbocatalog/&srcrwr=SEARCH_SAAS_LISTINGS:SAAS_ANSWERS&page=4#p6039')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaAssertViewportView('autoscroll');
    });

    hermione.only.notIn('safari13');
    it('Не должен подскроливать к первым четырём товарам с первой страницы', function() {
        return this.browser
            .url('/turbo?srcrwr=SEARCH_SAAS_LISTINGS:SAAS_ANSWERS&text=logomebel.ru/yandexturbocatalog/')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            // Скролим так, чтобы второй ряд товаров оказался в верхней половине окна.
            .yaScrollPageBy(500)
            // Задержка на тротлинг, чтобы убедиться, что адрес не поменялся позднее.
            // Не дотянем мы до полночи, Нас накрыл зенитный шквал...
            .pause(1100)
            .getUrl().then(function(url) {
                const u = new URL(url);
                return u.hash === '';
            });
    });

    hermione.only.notIn('safari13');
    it('Изменение номера товара по мере скрола вниз', function() {
        return this.browser
            .url('/turbo?text=logomebel.ru%2Fyandexturbocatalog%2F&srcrwr=SEARCH_SAAS_LISTINGS:SAAS_ANSWERS&page=4')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            // Скролим ниже немного больше, чем на один ряд.
            .yaScrollPageBy(300)
            .yaWaitUntil('Номер товара не поменялся', function() {
                return this.getUrl().then(function(url) {
                    // Проверка хэша одного из двух товаров, расположенных на одной строке.
                    return url.indexOf('#p8186') > -1 || url.indexOf('#p6793') > -1;
                });
            }, 3000, 250);
    });

    hermione.only.notIn('safari13');
    it('Сохранение товара после его выбора', function() {
        return this.browser
            .url('/turbo?text=logomebel.ru%2Fyandexturbocatalog%2F&srcrwr=SEARCH_SAAS_LISTINGS:SAAS_ANSWERS')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaIndexify(PO.blocks.products.item())
            .click(PO.blocks.products.item4.link())
            .yaWaitForVisible(PO.page(), 'Страница товара не загрузилась')
            .back()
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась при возвращении')
            .yaAssertViewportView('restore');
    });
});
