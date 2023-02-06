describe('Lego-Link', function() {
    it('Default', function() {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=Lego%20Link&selectedStory=Default')
            .assertView('Default', selector);
    });
});
