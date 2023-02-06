describe('NotFound', () => {
    it('page view', function() {
        return this.browser
            .url('/broken-url')
            .assertView('NotFoundPage', ['body']);
    });
});
