describe('Problem-settings---Compilation-controls', function () {
    it('Disabled', function () {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Problem%20settings%20%7C%20Compilation%20controls&selectedStory=Disabled',
            )
            .assertView('Disabled', selector);
    });
});
