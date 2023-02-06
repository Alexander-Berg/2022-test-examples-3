specs({
    feature: 'LcOffsets',
}, () => {
    function checkSize(browser, size) {
        return browser
            .url(`/turbo?stub=lcoffsets/size-${size}.json`)
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.lcOffsets());
    }

    hermione.only.notIn('safari13');
    it('Размер XS', function() {
        return checkSize(this.browser, 'xs');
    });
    hermione.only.notIn('safari13');
    it('Размер S', function() {
        return checkSize(this.browser, 's');
    });
    hermione.only.notIn('safari13');
    it('Размер M', function() {
        return checkSize(this.browser, 'm');
    });
    hermione.only.notIn('safari13');
    it('Размер L', function() {
        return checkSize(this.browser, 'l');
    });
    hermione.only.notIn('safari13');
    it('Размер XL', function() {
        return checkSize(this.browser, 'xl');
    });
});
