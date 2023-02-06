describe('Counter', function() {
    it('default', function() {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=Counter&selectedStory=default')
            .assertView('default', selector);
    });
});
