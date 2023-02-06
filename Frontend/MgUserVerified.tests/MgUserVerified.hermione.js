specs({
    feature: 'MgUserVerified',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид', function() {
        return this.browser
            .url('/turbo?stub=mguserverified/default.json')
            .yaWaitForVisible(PO.mgUsersVerified(), 'Страница не загрузилась')
            .assertView('plain', PO.mgUsersVerified());
    });
});
