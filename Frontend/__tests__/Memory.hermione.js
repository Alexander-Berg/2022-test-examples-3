describe('Suffix-Value', function() {
    it('Memory', function() {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=Suffix%20Value&selectedStory=Memory')
            .assertView('Memory', selector);
    });
});
