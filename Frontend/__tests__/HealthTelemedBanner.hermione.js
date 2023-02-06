specs({
    feature: 'Яндекс.Здоровье',
}, () => {
    // TODO: HEALTH-3038: [desktop] Покрыть баннер телемеда тестами
    hermione.only.in(['iphone', 'chrome-phone', 'searchapp'], 'Не показать с шапкой на десктопе');
    hermione.only.notIn('safari13');
    it('Баннер телемеда', function() {
        return this.browser
            .url('?stub=healthtelemedbanner%2Fdefault.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .execute(selector => {
                const banner = document.querySelector(selector);
                if (banner) {
                    banner.style.cssText = 'display: block !important; box-shadow: none;';
                }
            }, PO.healthTelemedBanner())
            .assertView('default', PO.healthTelemedBanner())
            .click(PO.healthTelemedBanner.closeButton())
            .yaWaitForHidden(PO.healthTelemedBanner(), 'Баннер телемеда не закрылся');
    });
});
