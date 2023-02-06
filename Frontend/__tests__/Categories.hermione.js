const { URL } = require('url');

specs({
    feature: 'Categories',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид блока', function() {
        return this.browser
            .url('/turbo?stub=categories/default.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.blocks.categories())
            .yaIndexify(PO.blocks.categories.item())
            .yaCheckLink({
                message: 'Ссылка должна быть кликабельна',
                selector: PO.blocks.categories.firstCategory.link(),
                target: '_self',
                url: {
                    href: '/turbo?stub=productspage/category.json',
                    ignore: ['protocol', 'hostname'],
                },
            });
    });

    hermione.only.notIn('safari13');
    it('Разворачивание скрытых категорий', function() {
        return this.browser
            .url('/turbo?stub=categories/default.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .elements(PO.blocks.categories.itemLink())
            .then(({ value }) => {
                assert.lengthOf(value, 2, 'В меню раскрыто больше 2х ссылок и одной button');
            })
            .click(PO.blocks.categories.more())
            .elements(PO.blocks.categories.itemLink())
            .then(({ value }) => {
                assert.lengthOf(value, 14, 'Раскрылось не 12 пунктов');
            });
    });

    hermione.only.notIn('safari13');
    it('Разворачивание скрытых категорий, заданное параметром', function() {
        return this.browser
            .url('/turbo?stub=categories/with-custom-params.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .elements(PO.blocks.categories.itemLink())
            .then(({ value }) => {
                assert.lengthOf(value, 2, 'В меню раскрыто больше 2х ссылок');
            })
            .click(PO.blocks.categories.more())
            .elements(PO.blocks.categories.itemLink())
            .then(({ value }) => {
                assert.lengthOf(value, 4, 'Раскрылось не 2 пункта');
            })
            .click(PO.blocks.categories.more())
            .yaShouldNotBeVisible(PO.blocks.categories.more())
            .assertView('plain', PO.blocks.categories());
    });

    hermione.only.in(['chrome-phone'], 'Ускоряем браузеронезависимые тесты');
    hermione.only.notIn('safari13');
    it('Восстановление количества развёрнутых категорий', function() {
        return this.browser
            .url('/turbo?stub=categories/with-custom-params.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .click(PO.blocks.categories.more())
            .refresh()
            .elements(PO.blocks.categories.itemLink())
            .then(({ value }) => {
                assert.lengthOf(value, 4, 'После перезагрузки не сохранились развернутые категории');
            })
            .click(PO.blocks.categories.more())
            .refresh()
            .elements(PO.blocks.categories.itemLink())
            .then(({ value }) => {
                assert.lengthOf(value, 6, 'После повторной перезагрузки не сохранились развернутые категории');
            });
    });

    hermione.only.in(['chrome-phone'], 'Ускоряем браузеронезависимые тесты');
    hermione.only.notIn('safari13');
    it('Восстановление количества развёрнутых категорий в оверлее', async function() {
        return this.browser
            .url('overlay?urls=/turbo?stub=categories/with-custom-params.json')
            .click('a')
            .element(PO.turboOverlay.iframe()).then(function({ value }) {
                return this.frame(value);
            })
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .click(PO.blocks.categories.more())
            .frameParent()
            .getUrl().then(function(url) {
                assert.isTrue(
                    new URL(url).searchParams.has('catcutlimit'),
                    'Родительскому окну не установился параметр catcutlimit',
                );
            });
    });
});
