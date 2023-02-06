const STORY_BOOK_URL = '/static/storybook';

module.exports = async function yaOpenComponent(this: WebdriverIO.Browser, path: string) {
    const storyUrl = `${STORY_BOOK_URL}/iframe.html?path=/story/${path}`;
    await this.url(storyUrl);
};
