describe('Contest-Problemset---Privacy', function() {
    it('Without-hint', function() {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Contest%20Problemset%20%7C%20Privacy&selectedStory=Without-hint',
            )
            .assertView('Without-hint', selector);
    });
});
