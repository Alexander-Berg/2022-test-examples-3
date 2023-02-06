describe('Contest-Settings---Packages-List', function() {
    it('Default', function() {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Contest%20Settings%20%7C%20Packages%20List&selectedStory=Default',
            )
            .assertView('Default', selector);
    });
});
