describe('Problems---Generators', function() {
    it('Default', function() {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Problems%20%7C%20Generators&selectedStory=Default',
            )
            .assertView('Default', selector, {
                screenshotTimeout: 5000,
            });
    });
});
