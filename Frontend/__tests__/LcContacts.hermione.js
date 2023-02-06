specs({
    feature: 'LcContacts',
}, () => {
    hermione.only.notIn('safari13');
    it('Рендер LcContacts', function() {
        return this.browser
            .url('/turbo?stub=lccontacts/default.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaWaitForVisible(PO.lcContacts(), 'На странице нет LcContacts')
            .assertView('full', PO.lcContacts());
    });
});
