specs({
    feature: 'EcomHeader',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид хедера', function() {
        return this.browser
            .url('/turbo?stub=ecomheader%2Fecomheader.json&exp_flags=turboforms_endpoint=/')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.page())
            .click(PO.turboSandwichMenu.button())
            .yaWaitForVisible(PO.turboSandwichMenuContainer())
            .yaIndexify(PO.turboSandwichMenuContainer.content.link())
            .yaShouldBeVisible(PO.turboSandwichMenuContainer.content.firstLink());
    });

    hermione.only.notIn('safari13');
    it('Внешний вид стилизованного хедера', function() {
        return this.browser
            .url('/turbo?stub=ecomheader%2Fecomheader-styled.json&exp_flags=turboforms_endpoint=/')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.page());
    });

    hermione.only.notIn('safari13');
    it('Внешний вид стилизованного хедера - различные комбинации логотипов', function() {
        return this.browser
            .url('/turbo?stub=ecomheader%2Fecomheader-logo-combinations.json&exp_flags=turboforms_endpoint=/')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.page());
    });

    hermione.only.notIn('safari13');
    it('Без меню, но с авторизацией', function() {
        return this.browser
            .url('/turbo?stub=ecomheader%2Fwith-auth.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .click(PO.turboSandwichMenu.button())
            .yaWaitForVisible(PO.turboSandwichMenuContainer())
            .yaWaitForHidden(PO.blocks.turboAuthSpinner())
            .yaWaitForVisible(PO.turboAuth())
            .assertView('menu', PO.turboSandwichMenuContainer.content());
    });
});
