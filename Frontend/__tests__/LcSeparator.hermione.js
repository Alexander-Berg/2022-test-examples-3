specs({
    feature: 'LcSeparator',
}, () => {
    hermione.only.notIn('safari13');
    it('Обычный разделитель', function() {
        return this.browser
            .url('/turbo?stub=lcseparator/default.json')
            .yaWaitForVisible(PO.lcSeparator(), 'Разделитель не появился')
            .assertView('plain', PO.lcSeparator());
    });

    hermione.only.notIn('safari13');
    it('Разделитель с маленькой шириной', function() {
        return this.browser
            .url('/turbo?stub=lcseparator/small.json')
            .yaWaitForVisible(PO.lcSeparator(), 'Разделитель не появился')
            .assertView('small', PO.lcSeparator());
    });
});
