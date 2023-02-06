describe('Suggest', function() {
    // eslint-disable-next-line mocha/no-skipped-tests
    it.skip('empty-result', function() {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=Suggest&selectedStory=empty-result')
            .assertView('empty-result', selector);
    });
});
