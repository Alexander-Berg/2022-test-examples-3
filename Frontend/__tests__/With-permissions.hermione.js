describe('Permissions---Changer', function() {
    it('With-permissions', function() {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Permissions%20%7C%20Changer&selectedStory=With-permissions',
            )
            .assertView('With-permissions', selector);
    });
});
