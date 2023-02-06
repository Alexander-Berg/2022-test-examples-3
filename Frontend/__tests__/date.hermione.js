specs({
    feature: 'date',
}, () => {
    hermione.only.notIn('safari13');
    it('Базовый формат даты', function() {
        return this.browser
            .url('/turbo?stub=date/default.json')
            .assertView('default', PO.blocks.date());
    });

    hermione.only.notIn('safari13');
    it('Сокращеный формат даты', function() {
        return this.browser
            .url('/turbo?stub=date/short.json')
            .assertView('short', PO.blocks.date());
    });

    hermione.only.notIn('safari13');
    it('Формат даты со временем', function() {
        return this.browser
            .url('/turbo?stub=date/up-to-minutes.json')
            .assertView('up-to-minutes', PO.blocks.date());
    });
});
