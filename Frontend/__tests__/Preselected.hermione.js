describe('Tab-Page', function() {
    // eslint-disable-next-line mocha/no-skipped-tests
    it.skip('Preselected', function() {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=Tab%20Page&selectedStory=Preselected')
            .assertView('Preselected', selector);
    });
});
