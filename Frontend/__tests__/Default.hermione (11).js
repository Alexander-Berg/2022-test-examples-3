describe('Contest-Problemset---Main', function() {
    it('Default', function() {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Contest%20Problemset%20%7C%20Main&selectedStory=Default',
            )
            .assertView('Default', selector);
    });
});
