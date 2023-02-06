hermione.skip.in('searchapp', 'Тест не проходится - Проверка на ошибки в клиентском коде завершилась неудачно');
specs({
    feature: 'LcBorders',
}, () => {
    hermione.only.notIn('safari13');
    it('Границы показываются корректно', function() {
        return this.browser
            .url('/turbo?stub=lcborders/default.json')
            .yaWaitForVisible(PO.lcBorders(), 'Страница не отобразилась')
            .assertView('plain', PO.lcBorders());
    });

    hermione.only.notIn('safari13');
    it('Границы показываются корректно для секции с фоном', function() {
        return this.browser
            .url('/turbo?stub=lcborders/with-background.json')
            .yaWaitForVisible(PO.lcBorders(), 'Страница не отобразилась')
            .assertView('plain', PO.lcBorders());
    });
});
