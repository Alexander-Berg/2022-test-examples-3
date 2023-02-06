describe('Contest-Settings---Submissions', function() {
    it('maxSubmissionsPerSecond-not-specified', function() {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Contest%20Settings%20%7C%20Submissions&selectedStory=maxSubmissionsPerSecond-not-specified',
            )
            .assertView('maxSubmissionsPerSecond-not-specified', selector);
    });
});
