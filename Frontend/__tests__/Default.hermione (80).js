describe('Problems---Validators', function() {
    it('Default', function() {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Problems%20%7C%20Validators&selectedStory=Default',
            )
            .assertView('Default', selector);
    });
});
