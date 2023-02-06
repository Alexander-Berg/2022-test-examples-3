describe('Contest-Review---General', function() {
    it('Empty', function() {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Contest%20Review%20%7C%20General&selectedStory=Empty',
            )
            .assertView('Empty', selector);
    });
});
