describe('Tab-Nav', function() {
    // eslint-disable-next-line mocha/no-skipped-tests
    it.skip('main', function() {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=Tab%20Nav&selectedStory=main')
            .assertView('main', selector);
    });
});
