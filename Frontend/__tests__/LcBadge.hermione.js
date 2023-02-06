specs({
    feature: 'LcBadge',
}, () => {
    hermione.only.notIn('safari13');
    it('Бейджи Apple, Google, Microsoft на языке ru', function() {
        return this.browser
            .url('/turbo?stub=lcbadge/lang_ru.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('lang_ru', PO.lcPage());
    });

    hermione.only.notIn('safari13');
    it('Бейджи Apple, Google, Microsoft на языке be', function() {
        return this.browser
            .url('/turbo?stub=lcbadge/lang_be.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('lang_be', PO.lcPage());
    });

    hermione.only.notIn('safari13');
    it('Бейджи Apple, Google, Microsoft на языке en', function() {
        return this.browser
            .url('/turbo?stub=lcbadge/lang_en.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('lang_en', PO.lcPage());
    });

    hermione.only.notIn('safari13');
    it('Бейджи Apple, Google, Microsoft на языке kz', function() {
        return this.browser
            .url('/turbo?stub=lcbadge/lang_kz.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('lang_kz', PO.lcPage());
    });

    hermione.only.notIn('safari13');
    it('Бейджи Apple, Google, Microsoft на языке tr', function() {
        return this.browser
            .url('/turbo?stub=lcbadge/lang_tr.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('lang_tr', PO.lcPage());
    });

    hermione.only.notIn('safari13');
    it('Бейджи Apple, Google, Microsoft на языке uk', function() {
        return this.browser
            .url('/turbo?stub=lcbadge/lang_uk.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('lang_uk', PO.lcPage());
    });
});
