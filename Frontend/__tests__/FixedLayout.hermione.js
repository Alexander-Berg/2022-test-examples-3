describe('Table', function() {
    it('FixedLayout', function() {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=Table&selectedStory=FixedLayout')
            .assertView('FixedLayout', selector);
    });
});
