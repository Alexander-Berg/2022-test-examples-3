module.exports = async function(action, timeout = 5000) {
    const oldUrl = await this.getUrl();
    await action();
    return this.waitUntil(() => {
        return this.getUrl().then(newURL => {
            return newURL !== oldUrl;
        });
    }, timeout);
};
