describe('Tab-Page', function() {
    // eslint-disable-next-line mocha/no-skipped-tests
    it.skip('Default', function() {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=Tab%20Page&selectedStory=Default')
            .assertView('Default', selector);
    });
});
