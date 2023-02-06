describe('Contest-Settings---Report', function() {
    it('default', function() {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Contest%20Settings%20%7C%20Report&selectedStory=default',
            )
            .assertView('default', selector);
    });
});
