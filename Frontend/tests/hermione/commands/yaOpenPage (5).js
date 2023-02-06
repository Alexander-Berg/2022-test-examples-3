module.exports = function(path) {
    return this
        .url(`/tutor${path}`)
        .yaWaitForLoadPage();
};
