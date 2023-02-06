module.exports = new Proxy({}, {
    get: (target, property) => target[property] || require(`${__dirname}/${property}`),
});
