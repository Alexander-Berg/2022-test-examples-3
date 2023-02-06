describe('Layout', function() {
    it('dashboard', function() {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=Layout&selectedStory=dashboard')
            .assertView('dashboard', selector);
    });
});
