describe('Contest-Problemset---Main', function() {
    it('No-permission', function() {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Contest%20Problemset%20%7C%20Main&selectedStory=No-permission',
            )
            .assertView('No-permission', selector);
    });
});
