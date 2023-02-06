describe('Problems---Create-Test', function() {
    it('Default', function() {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Problems%20%7C%20Create%20Test&selectedStory=Default',
            )
            .assertView('Default', selector);
    });
});
