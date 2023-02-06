describe('Problemset-settings---Access', function() {
    // eslint-disable-next-line mocha/no-skipped-tests
    it.skip('default', function() {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Problemset%20settings%20%7C%20Access&selectedStory=default',
            )
            .assertView('default', selector);
    });
});
