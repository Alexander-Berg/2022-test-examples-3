module.exports = async function ywGetBrowserLog() {
    const logResult = await this.log('browser');

    return logResult.value;
};
