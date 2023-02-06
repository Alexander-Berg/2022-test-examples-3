describe('Problemset-settings---Common', function() {
    it('default', function() {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Problemset%20settings%20%7C%20Common&selectedStory=default',
            )
            .assertView('default', selector);
    });
});
