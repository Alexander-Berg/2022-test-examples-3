describe('Problems---Script', function() {
    // eslint-disable-next-line mocha/no-skipped-tests
    it.skip('Loading-attempts', function() {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Problems%20%7C%20Script&selectedStory=Loading-attempts',
            )
            .assertView('Loading-attempts', selector, {
                ignoreElements: '.Spin2',
            });
    });
});
