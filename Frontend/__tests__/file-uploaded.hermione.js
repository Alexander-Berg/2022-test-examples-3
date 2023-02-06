describe('File-Uploader', function() {
    it('file-uploaded', function() {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=File%20Uploader&selectedStory=file-uploaded')
            .moveToObject(`${selector} > *`)
            .assertView('file-uploaded', selector);
    });
});
