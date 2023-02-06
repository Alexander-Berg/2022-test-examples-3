describe('Tab-Nav', function() {
    // eslint-disable-next-line mocha/no-skipped-tests
    it.skip('secondary-size-s', function() {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=Tab%20Nav&selectedStory=secondary-size-s')
            .assertView('secondary-size-s', selector);
    });
});
