describe('Contest-Settings---PerformanceTime', function() {
    it('total-usual', function() {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Contest%20Settings%20%7C%20PerformanceTime&selectedStory=total-usual',
            )
            .assertView('total-usual', selector);
    });
});
