describe('Contest-Settings---Compilers', function() {
    it('disabled', function() {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Contest%20Settings%20%7C%20Compilers&selectedStory=disabled',
            )
            .assertView('disabled', selector);
    });
});
