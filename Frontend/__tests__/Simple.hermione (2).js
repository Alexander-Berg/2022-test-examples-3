describe('Pager', function() {
    // eslint-disable-next-line mocha/no-skipped-tests
    it.skip('Simple', function() {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=Pager&selectedStory=Simple')
            .assertView('Simple', selector);
    });
});
