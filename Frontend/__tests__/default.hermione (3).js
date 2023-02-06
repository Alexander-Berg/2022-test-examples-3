describe('Add-text-input', function() {
    it('default', function() {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=Add%20text%20input&selectedStory=default')
            .assertView('default', selector);
    });
});
