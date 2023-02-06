describe('Contest-Submissions---DownloadArchive', function () {
    it('Generating', function () {
        const selector = '.story-container';
        return this.browser
            .url(
                'storybook/iframe.html?selectedKind=Contest%20Submissions%20%7C%20DownloadArchive&selectedStory=Generating',
            )
            .assertView('Generating', selector);
    });
});
