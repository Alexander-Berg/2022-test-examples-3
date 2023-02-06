describe('Menu', function() {
    it('default', function() {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=Menu&selectedStory=default')
            .assertView('default', selector);
    });
});
