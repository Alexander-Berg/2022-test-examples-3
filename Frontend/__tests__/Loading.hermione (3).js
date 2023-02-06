describe('Table', function() {
    // eslint-disable-next-line mocha/no-skipped-tests
    it.skip('Loading', function() {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=Table&selectedStory=Loading')
            .assertView('Loading', selector, {
                ignoreElements: '.Spin2',
            });
    });
});
