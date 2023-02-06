describe('Icon-Control', function() {
    it('can-be-button', function() {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=Icon%20Control&selectedStory=can-be-button')
            .assertView('can-be-button', selector);
    });
});
