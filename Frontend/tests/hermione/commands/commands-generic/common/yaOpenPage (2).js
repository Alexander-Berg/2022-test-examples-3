module.exports = function(path) {
    return this
        .url(`/searchapp${path}`)
        .yaWaitForVisible('.Layout');
};
