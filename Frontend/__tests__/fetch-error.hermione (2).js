describe('Suggest', function() {
    // eslint-disable-next-line mocha/no-skipped-tests
    it.skip('fetch-error', function() {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=Suggest&selectedStory=fetch-error')
            .assertView('fetch-error', selector);
    });
});
