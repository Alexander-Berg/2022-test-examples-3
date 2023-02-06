describe('serp-header.web4.button-in-focus', () => {
    it('websearch-button-in-focus', function() {
        return this.browser
            .url('dist/examples/desktop/serp-header/serp-header.web4.base.html')
            .moveToObject('.websearch-button:nth-of-type(1)')
            .buttonDown()
            .assertView('focused', '.search2:nth-of-type(1)');
    });
});
