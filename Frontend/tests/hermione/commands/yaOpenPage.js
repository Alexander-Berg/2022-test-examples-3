module.exports = function(path) {
    return this
        .url(`/butterfly${path}`)
        .yaWaitForLoadPage();
};
