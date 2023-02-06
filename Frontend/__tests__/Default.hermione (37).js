describe('Errors---Not-Found', function() {
    it('Default', function() {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Errors%20%7C%20Not%20Found&selectedStory=Default',
            )
            .assertView('Default', selector);
    });
});
