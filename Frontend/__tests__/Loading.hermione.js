describe('Pager', function() {
    // eslint-disable-next-line mocha/no-skipped-tests
    it.skip('Loading', function() {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=Pager&selectedStory=Loading')
            .assertView('Loading', selector, {
                allowViewportOverflow: true,
            });
    });
});
