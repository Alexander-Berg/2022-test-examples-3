xdescribe('layout-compose-popular-contacts', function() {
    it('Должен вернуть вид популярных контактов, если он есть включен в настройках', function() {
        this.mSettings.isSet.returns(true);
        this.mComposePopularContacts.isAnyUnusedContact.returns(true);

        var layout = ns.layout.page('layout-compose-popular-contacts');

        expect(layout[ 'compose-popular-contacts-box' ].views).to.have.keys('compose-popular-contacts');
    });

    it('Не должен возвращать вид популярных контактов, если он выключен в настройках', function() {
        this.mSettings.isSet.returns(false);
        this.mComposePopularContacts.isAnyUnusedContact.returns(true);
        var layout = ns.layout.page('layout-compose-popular-contacts');

        expect(layout[ 'compose-popular-contacts-box' ].views).to.not.have.keys('compose-popular-contacts');
    });

    it('Не должен возвращать вид популярных контактов, если нет ни одного не задействованного контакта', function() {
        this.mSettings.isSet.returns(true);
        this.mComposePopularContacts.isAnyUnusedContact.returns(false);
        var layout = ns.layout.page('layout-compose-popular-contacts');

        expect(layout[ 'compose-popular-contacts-box' ].views).to.not.have.keys('compose-popular-contacts');
    });
});

