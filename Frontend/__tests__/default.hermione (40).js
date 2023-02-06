describe('File-Uploader', function() {
    it('default', function() {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=File%20Uploader&selectedStory=default')
            .moveToObject(`${selector} > *`)
            .assertView('default', selector);
    });
});
