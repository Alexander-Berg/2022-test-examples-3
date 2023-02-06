describe('Contest-Settings---Common', function() {
    it('placeholder', function() {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Contest%20Settings%20%7C%20Common&selectedStory=placeholder',
            )
            .assertView('placeholder', selector);
    });
});
