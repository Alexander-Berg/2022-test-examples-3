describe('Header', function() {
    it('default', function() {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=Header&selectedStory=default')
            .assertView('default', selector);
    });
});
