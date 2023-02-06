describe('Problem-settings---Compilation-controls', function () {
    it('No-status', function () {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Problem%20settings%20%7C%20Compilation%20controls&selectedStory=No-status',
            )
            .assertView('No-status', selector);
    });
});
