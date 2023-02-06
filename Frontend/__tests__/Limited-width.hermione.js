describe('Table', function() {
    it('Limited-width', function() {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=Table&selectedStory=Limited-width')
            .assertView('Limited-width', selector);
    });
});
