specs({
    feature: 'LcContacts',
}, () => {
    it('Рендер LcContacts с LcContactsPhone', function() {
        return this.browser
            .url('/turbo?stub=lccontacts/with-lc-contacts-phone.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaWaitForVisible(PO.lcContacts(), 'На странице нет LcContacts')
            .assertView('plain', PO.lcContacts())
            .click(PO.lcContactsPhoneInscription())
            .yaWaitForHidden(PO.lcContactsPhoneInscription(), 'Подпись не скрылась')
            .assertView('opened', PO.lcContacts());
    });
});
