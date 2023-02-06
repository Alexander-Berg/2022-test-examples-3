describe('Contest-Settings---Compilers', function() {
    it('all-checked', function() {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Contest%20Settings%20%7C%20Compilers&selectedStory=all-checked',
            )
            .assertView('all-checked', selector);
    });
});
