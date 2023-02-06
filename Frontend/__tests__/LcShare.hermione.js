specs({
    feature: 'LcShare',
}, () => {
    hermione.only.notIn('safari13');
    it('Обычные поделяшки', function() {
        return this.browser
            .url('/turbo?stub=lcshare/default.json')
            .yaWaitForVisible('.ya-share2', 'Поделяшки не загрузилась')
            .assertView('plain', PO.lcShare());
    });

    hermione.only.notIn('safari13');
    it('Маленькие круглые цветные поделяшки', function() {
        return this.browser
            .url('/turbo?stub=lcshare/smallRoundOriginal.json')
            .yaWaitForVisible('.ya-share2', 'Поделяшки не загрузилась')
            .assertView('plain', PO.lcShare());
    });

    hermione.only.notIn('safari13');
    it('Маленькие круглые черные поделяшки', function() {
        return this.browser
            .url('/turbo?stub=lcshare/smallRoundBlack.json')
            .yaWaitForVisible('.ya-share2', 'Поделяшки не загрузилась')
            .assertView('plain', PO.lcShare());
    });

    hermione.only.notIn('safari13');
    it('Маленькие квадратные белые поделяшки', function() {
        return this.browser
            .url('/turbo?stub=lcshare/smallSquareWhite.json')
            .yaWaitForVisible('.ya-share2', 'Поделяшки не загрузилась')
            .assertView('plain', PO.lcShare());
    });

    hermione.only.notIn('safari13');
    it('Средние квадратные белые поделяшки', function() {
        return this.browser
            .url('/turbo?stub=lcshare/mediumSquareWhite.json')
            .yaWaitForVisible('.ya-share2', 'Поделяшки не загрузилась')
            .assertView('plain', PO.lcShare());
    });

    hermione.only.notIn('safari13');
    it('Средние квадратные черные поделяшки', function() {
        return this.browser
            .url('/turbo?stub=lcshare/mediumSquareBlack.json')
            .yaWaitForVisible('.ya-share2', 'Поделяшки не загрузилась')
            .assertView('plain', PO.lcShare());
    });

    hermione.only.notIn('safari13');
    it('Средние круглые цветные поделяшки', function() {
        return this.browser
            .url('/turbo?stub=lcshare/mediumRoundOriginal.json')
            .yaWaitForVisible('.ya-share2', 'Поделяшки не загрузилась')
            .assertView('plain', PO.lcShare());
    });

    hermione.only.notIn('safari13');
    it('Большие круглые цветные поделяшки', function() {
        return this.browser
            .url('/turbo?stub=lcshare/bigRoundOriginal.json')
            .yaWaitForVisible('.ya-share2', 'Поделяшки не загрузилась')
            .assertView('plain', PO.lcShare());
    });

    hermione.only.notIn('safari13');
    it('Большие круглые белые поделяшки', function() {
        return this.browser
            .url('/turbo?stub=lcshare/bigRoundWhite.json')
            .yaWaitForVisible('.ya-share2', 'Поделяшки не загрузилась')
            .assertView('plain', PO.lcShare());
    });

    hermione.only.notIn('safari13');
    it('Большие квадратные черные поделяшки', function() {
        return this.browser
            .url('/turbo?stub=lcshare/bigSquareBlack.json')
            .yaWaitForVisible('.ya-share2', 'Поделяшки не загрузилась')
            .assertView('plain', PO.lcShare());
    });
});
