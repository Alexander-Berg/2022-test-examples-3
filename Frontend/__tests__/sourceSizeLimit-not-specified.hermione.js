describe('Contest-Settings---Submissions', function() {
    it('sourceSizeLimit-not-specified', function() {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Contest%20Settings%20%7C%20Submissions&selectedStory=sourceSizeLimit-not-specified',
            )
            .assertView('sourceSizeLimit-not-specified', selector);
    });
});
