specs({
    feature: 'Breadcrumbs',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид блока', function() {
        return this.browser
            .url('/turbo?stub=breadcrumbs/default.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.blocks.breadcrumbs())
            .yaCheckBaobabServerCounter({
                path: '$page.$main.$result.breadcrumbs.link',
            })
            .yaCheckBaobabCounter(PO.blocks.breadcrumbs.link(), {
                path: '$page.$main.$result.breadcrumbs.link',
            });
    });

    hermione.only.notIn('safari13');
    it('Внешний вид блока с длиным текстом в ссылке', function() {
        return this.browser
            .url('/turbo?stub=breadcrumbs/long-text.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.blocks.breadcrumbs());
    });

    hermione.only.notIn('safari13');
    it('Внешний вид блока Ecommerce дизайн', function() {
        return this.browser
            .url('/turbo?stub=breadcrumbs/default.json&exp_flags=ecommerce-design')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.blocks.breadcrumbs());
    });

    hermione.only.notIn('safari13');
    it('Внешний вид блока Ecommerce дизайн с длинным текстом', function() {
        return this.browser
            .url('/turbo?stub=breadcrumbs/long-text.json&exp_flags=ecommerce-design')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.blocks.breadcrumbs());
    });
});
