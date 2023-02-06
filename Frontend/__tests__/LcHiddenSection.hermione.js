hermione.skip.in(/.*/);
specs({
    feature: 'LcHiddenSection',
}, () => {
    hermione.only.notIn('safari13');
    it('Скрытая секция', function() {
        return this.browser
            .url('/turbo?stub=lchiddensection/default.json')
            .yaWaitForVisible(PO.lcHiddenSection(), 'Скрытая секция не появилась')
            .assertView('plain', PO.lcHiddenSection());
    });
    hermione.only.notIn('safari13');
    it('Активная скрытая секция', function() {
        return this.browser
            .url('/turbo?stub=lchiddensection/focused.json')
            .yaWaitForVisible(PO.lcHiddenSection(), 'Скрытая секция не появилась')
            .assertView('plain', PO.lcHiddenSection());
    });
});
