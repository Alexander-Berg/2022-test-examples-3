describe('Counter', function() {
    it('associated-with-label', function() {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=Counter&selectedStory=associated-with-label')
            .assertView('associated-with-label', selector);
    });
});
