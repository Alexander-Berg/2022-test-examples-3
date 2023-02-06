describe('Table', function() {
    it('FixedHeader-with-scroll', function() {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=Table&selectedStory=FixedHeader-with-scroll')
            .assertView('FixedHeader-with-scroll', selector);
    });
});
