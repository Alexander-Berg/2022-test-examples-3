specs({
    feature: 'Key-value',
}, () => {
    hermione.only.notIn('safari13');
    it('Проверка ссылок в key-value', function() {
        return this.browser
            .url('/turbo?stub=keyvalue%2Fdefault.json')
            .yaWaitForVisible(PO.turboKeyValue())
            .yaCheckLink({
                selector: PO.turboKeyValue.link(),
                message: 'Сломана ссылка в key-value',
            })
            .assertView('plain', PO.turboKeyValue());
    });
});
