describe('Pagination', function() {
    it('default', function() {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=Pagination&selectedStory=default')
            .assertView('default', selector);
    });
});
