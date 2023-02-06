describe('Contest-Settings---Report', function() {
    it('inverted', function() {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Contest%20Settings%20%7C%20Report&selectedStory=inverted',
            )
            .assertView('inverted', selector);
    });
});
