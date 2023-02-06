describe('File-Uploader', function() {
    it('without-text', function() {
        const selector = '.story-container';
        return this.browser
            .url('storybook/iframe.html?selectedKind=File%20Uploader&selectedStory=without-text')
            .moveToObject(`${selector} > *`)
            .assertView('without-text', selector);
    });
});
