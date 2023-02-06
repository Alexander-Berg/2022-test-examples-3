describe('Problems---Packages-List', function() {
    // eslint-disable-next-line mocha/no-skipped-tests
    it.skip('Default', function() {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Problems%20%7C%20Packages%20List&selectedStory=Default',
            )
            .assertView('Default', selector, {
                screenshotDelay: 3000,
            });
    });
});
