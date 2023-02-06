describe('Layout', function() {
    it('with-sidebar', function() {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=Layout&selectedStory=with-sidebar')
            .assertView('with-sidebar', selector);
    });
});
