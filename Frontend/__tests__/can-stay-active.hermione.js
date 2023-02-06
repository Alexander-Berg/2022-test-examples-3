describe('Icon-Control', function() {
    it('can-stay-active', function() {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=Icon%20Control&selectedStory=can-stay-active')
            .assertView('can-stay-active', selector);
    });
});
