describe('Problemset-Settings---Update-Problemset', function() {
    // eslint-disable-next-line mocha/no-skipped-tests
    it.skip('Default', function() {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Problemset%20Settings%20%7C%20Update%20Problemset&selectedStory=Default',
            )
            .assertView('Default', selector);
    });
});
