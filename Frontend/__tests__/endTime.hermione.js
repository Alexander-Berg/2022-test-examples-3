describe('Contest-Settings---PerformanceTime', function() {
    it('endTime', function() {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Contest%20Settings%20%7C%20PerformanceTime&selectedStory=endTime',
            )
            .assertView('endTime', selector, {
                screenshotTimeout: 5000,
            });
    });
});
