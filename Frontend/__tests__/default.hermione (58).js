describe('MessagePopup', function() {
    it('default', function() {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=MessagePopup&selectedStory=default')
            .assertView('default', selector);
    });
});
