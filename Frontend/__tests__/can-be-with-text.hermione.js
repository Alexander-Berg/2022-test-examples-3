describe('Icon-Control', function() {
    it('can-be-with-text', function() {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=Icon%20Control&selectedStory=can-be-with-text')
            .assertView('can-be-with-text', selector);
    });
});
