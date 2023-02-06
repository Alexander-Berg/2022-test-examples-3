specs({
    feature: 'ecom-one-click-buy-button',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид', function() {
        return this.browser
            .url('?stub=ecomoneclickbuybutton/default.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.oneClickBuyButton());
    });

    hermione.only.notIn('safari13');
    it('Цели метрики', function() {
        return this.browser
            .url('?stub=ecomoneclickbuybutton/default.json&exp_flags=analytics-disabled=0')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaWaitForVisible('.metrika_loaded', 'Не загрузилась метрика')
            .click(PO.oneClickBuyButton())
            .yaCheckMetrikaGoals({
                '11111111': ['open-check-out-form'],
            });
    });
});
