describe('Contest-Settings---PerformanceTime', function() {
    it('startTime', function() {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Contest%20Settings%20%7C%20PerformanceTime&selectedStory=startTime',
            )
            .assertView('startTime', selector);
    });
});
