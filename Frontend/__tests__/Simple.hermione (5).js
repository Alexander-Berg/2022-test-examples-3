describe('Table', function() {
    it('Simple', function() {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=Table&selectedStory=Simple')
            .assertView('Simple', selector);
    });
});
