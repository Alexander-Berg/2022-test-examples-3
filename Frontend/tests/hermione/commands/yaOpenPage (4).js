module.exports = function(path) {
    return this
        .url(`/video/quasar/${path}`)
        .yaWaitForLoadPage();
};
