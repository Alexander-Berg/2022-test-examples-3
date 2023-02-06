describe('Domains---List', function() {
    it('Default', function() {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=Domains%20%7C%20List&selectedStory=Default')
            .assertView('Default', selector);
    });
});
