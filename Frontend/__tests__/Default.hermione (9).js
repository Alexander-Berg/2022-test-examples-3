describe('Contest-Members---Role-Select', function() {
    it('Default', function() {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Contest%20Members%20%7C%20Role%20Select&selectedStory=Default',
            )
            .assertView('Default', selector);
    });
});
