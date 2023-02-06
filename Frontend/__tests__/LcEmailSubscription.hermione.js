specs({
    feature: 'LcEmailSubscription',
}, () => {
    hermione.only.notIn('safari13');
    it('Секция подписки на email рассылку', function() {
        return this.browser
            .url('/turbo?stub=lcemailsubscription/default.json')
            .yaWaitForVisible(PO.lcEmailSubscription(), 'Секция не появилась')
            .assertView('plain', PO.lcEmailSubscription());
    });
});
