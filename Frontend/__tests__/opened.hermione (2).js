describe('Markdown-Hint', function () {
    it('opened', function () {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=Markdown%20Hint&selectedStory=default')
            .click('.markdown-hint__icon')
            .assertView('opened', selector);
    });
});
