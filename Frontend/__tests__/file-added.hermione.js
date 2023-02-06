describe('File-Uploader', function() {
    it('file-added', function() {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=File%20Uploader&selectedStory=file-added')
            .moveToObject(`${selector} > *`)
            .assertView('file-added', selector);
    });
});
