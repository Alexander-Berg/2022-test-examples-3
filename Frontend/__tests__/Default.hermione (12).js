describe('Contest-Problemset---Conditions', function() {
    it('Default', function() {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Contest%20Problemset%20%7C%20Conditions&selectedStory=Default',
            )
            .assertView('Default', selector);
    });
});
