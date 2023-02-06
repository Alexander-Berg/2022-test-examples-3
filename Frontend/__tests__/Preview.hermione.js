describe('Table', function() {
    it('Preview', function() {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=Table&selectedStory=Preview')
            .assertView('Loading', selector);
    });
});
