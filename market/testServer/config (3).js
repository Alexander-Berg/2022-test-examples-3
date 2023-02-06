try {
    // eslint-disable-next-line global-require
    module.exports = require(`${process.cwd()}/analytics/testServer/.config.json`);
} catch (e) {
    module.exports = {port: 3002};
}
