describe('Icon-Control', function() {
    it('can-be-link', function() {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=Icon%20Control&selectedStory=can-be-link')
            .assertView('can-be-link', selector);
    });
});
