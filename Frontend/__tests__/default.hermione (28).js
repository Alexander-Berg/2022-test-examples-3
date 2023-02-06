describe('Context-Popup', function() {
    it('default', function() {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=Context%20Popup&selectedStory=default')
            .assertView('default', selector);
    });
});
