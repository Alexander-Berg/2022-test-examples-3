describe('Problems---Testsets', function() {
    it('Default', function() {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Problems%20%7C%20Testsets&selectedStory=Default',
            )
            .assertView('Default', selector);
    });
});
