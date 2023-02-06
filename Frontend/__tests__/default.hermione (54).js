describe('Markdown-Hint', function () {
    it('default', function () {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=Markdown%20Hint&selectedStory=default')
            .assertView('default', selector);
    });
});
