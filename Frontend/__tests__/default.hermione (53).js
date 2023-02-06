describe('Login', function() {
    it('default', function() {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=Login&selectedStory=default')
            .assertView('default', selector);
    });
});
