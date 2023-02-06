describe('File-Uploader', function() {
    it('uploading-error', function() {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=File%20Uploader&selectedStory=uploading-error')
            .moveToObject(`${selector} > *`)
            .assertView('uploading-error', selector);
    });
});
