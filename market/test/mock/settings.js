function Settings(contentRegion) {
    this.contentRegion = contentRegion;
}

Settings.prototype.getSetting = function (key) {
    return this[key];
};

module.exports = {
    DEFAULT: new Settings(),
    KIEV_CR: new Settings(143),
};
