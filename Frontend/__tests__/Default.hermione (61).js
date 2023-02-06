describe('Permissions---Changer', function() {
    it('Default', function() {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Permissions%20%7C%20Changer&selectedStory=Default',
            )
            .assertView('Default', selector);
    });
});
