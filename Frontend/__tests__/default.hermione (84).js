describe('Statements', function() {
    it('default', function() {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=Statements&selectedStory=default')
            .assertView('default', selector);
    });
});
