describe('TriggersList', () => {
    it('page view', function() {
        return this.browser
            .url('/')
            .assertView('plain', ['body']);
    });
});
