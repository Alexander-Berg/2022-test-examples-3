describe('Contest-Settings---Submissions', function () {
    it('maxSubmissionsPerSecond-specified', function () {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Contest%20Settings%20%7C%20Submissions&selectedStory=maxSubmissionsPerSecond-specified',
            )
            .assertView('maxSubmissionsPerSecond-specified', selector);
    });
});
