describe('Suggest', function() {
    // eslint-disable-next-line mocha/no-skipped-tests
    it.skip('default', function() {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=Suggest&selectedStory=default')
            .assertView('default', selector);
    });
});
