describe('Contest-Submissions---DownloadArchive', function () {
    it('Default', function () {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Contest%20Submissions%20%7C%20DownloadArchive&selectedStory=Default',
            )
            .assertView('Default', selector);
    });
});
