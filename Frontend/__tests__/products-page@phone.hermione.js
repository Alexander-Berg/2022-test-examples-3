specs({
    feature: 'ProductsPage',
}, () => {
    describe('Отступы между блоками', function() {
        hermione.only.in(['iphone'], 'Ускоряем браузеронезависимые тесты');
        hermione.only.notIn('safari13');
        it('Корень каталога', function() {
            return this.browser
                .url('/turbo?stub=productspage/index.json')
                .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
                .assertView('plain', PO.page(), {
                    ignoreElements: [
                        PO.blocks.ecomHeader(),
                        PO.blocks.formPresetSearch(),
                        PO.blocks.breadcrumbs(),
                        PO.blocks.title(),
                        PO.blocks.categories(),
                        PO.blocks.products(),
                        PO.blocks.ecomFooter(),
                    ],
                });
        });

        hermione.only.in(['iphone'], 'Ускоряем браузеронезависимые тесты');
        hermione.only.notIn('safari13');
        it('Категория', function() {
            return this.browser
                .url('/turbo?stub=productspage/category.json')
                .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
                .assertView('plain', PO.page(), {
                    ignoreElements: [
                        PO.blocks.ecomHeader(),
                        PO.blocks.formPresetSearch(),
                        PO.blocks.breadcrumbs(),
                        PO.blocks.title(),
                        PO.blocks.categories(),
                        PO.blocks.products(),
                        PO.blocks.ecomFooter(),
                    ],
                });
        });

        hermione.only.in(['iphone'], 'Ускоряем браузеронезависимые тесты');
        hermione.only.notIn('safari13');
        it('С саджестом', function() {
            return this.browser
                .url('/turbo?stub=productspage/index-with-suggest.json')
                .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
                .assertView('plain', PO.page(), {
                    ignoreElements: [
                        PO.blocks.ecomHeader(),
                        PO.productsSearch(),
                        PO.blocks.breadcrumbs(),
                        PO.blocks.title(),
                        PO.blocks.categories(),
                        PO.blocks.products(),
                        PO.blocks.ecomFooter(),
                    ],
                });
        });

        hermione.only.in(['iphone'], 'Ускоряем браузеронезависимые тесты');
        hermione.only.notIn('safari13');
        it('С поиском и заметкой', function() {
            return this.browser
                .url('/turbo?stub=productspage/index-with-notice.json')
                .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
                .assertView('plain', PO.page(), {
                    ignoreElements: [
                        PO.blocks.ecomHeader(),
                        PO.blocks.formPresetSearch(),
                        PO.blocks.notice(),
                        PO.blocks.breadcrumbs(),
                        PO.blocks.title(),
                        PO.blocks.categories(),
                        PO.blocks.products(),
                        PO.blocks.ecomFooter(),
                    ],
                });
        });
    });

    hermione.only.notIn('safari13');
    it('Внешний вид категории', function() {
        return this.browser
            .url('/turbo?stub=productspage/category.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.page());
    });

    hermione.only.notIn('safari13');
    it('Внешний вид с бейджом безопасности', function() {
        return this.browser
            .url('/turbo?stub=productspage/category-with-safe.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', [
                PO.blocks.ecomHeader(),
                PO.ecomSecureTransactionNotice(),
                PO.blocks.formPresetSearch(),
            ], {
                ignoreElements: [
                    PO.blocks.ecomHeader(),
                    PO.blocks.formPresetSearch(),
                    PO.blocks.notice(),
                    PO.blocks.breadcrumbs(),
                    PO.blocks.title(),
                    PO.blocks.categories(),
                    PO.blocks.products(),
                    PO.blocks.ecomFooter(),
                ],
            });
    });
});
