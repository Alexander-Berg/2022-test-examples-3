describe('Contest-Settings---Testing', function() {
    it('not-use-AC-not-OK', function() {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Contest%20Settings%20%7C%20Testing&selectedStory=not-use-AC-not-OK',
            )
            .assertView('not-use-AC-not-OK', selector);
    });
});
