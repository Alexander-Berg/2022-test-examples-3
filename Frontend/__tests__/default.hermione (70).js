describe('Problemsets---Brief', function() {
    it('default', function() {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Problemsets%20%7C%20Brief&selectedStory=default',
            )
            .assertView('default', selector);
    });
});
