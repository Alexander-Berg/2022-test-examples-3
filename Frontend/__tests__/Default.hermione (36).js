describe('Errors---No-Access', function() {
    it('Default', function() {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Errors%20%7C%20No%20Access&selectedStory=Default',
            )
            .assertView('Default', selector);
    });
});
