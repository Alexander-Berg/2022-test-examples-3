module.exports = async function yaOpenUrl(this: WebdriverIO.Browser, path: string) {
    await this.yaOpenPageByUrl(path);
    await this.yaWaitForPageLoad();
    await this.yaWaitForVisible('.agency-info__block', 6000, 'Info block not displayed');
};
