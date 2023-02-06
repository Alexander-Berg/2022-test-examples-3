describe('Counter', function() {
    it('positive-interval', function() {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=Counter&selectedStory=positive-interval')
            .assertView('positive-interval', selector);
    });
});
