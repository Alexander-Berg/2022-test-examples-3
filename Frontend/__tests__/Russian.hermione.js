describe('Logo', function() {
    it('Russian', function() {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=Logo&selectedStory=Russian')
            .assertView('Russian', selector);
    });
});
