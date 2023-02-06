module.exports = async function yaOpenOrd(this: WebdriverIO.Browser) {
    await this.yaOpenPageByUrl('/partner-office/1111/ord/reports');
    await this.yaWaitForPageLoad();
};
