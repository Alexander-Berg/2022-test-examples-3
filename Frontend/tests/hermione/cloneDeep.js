module.exports = function cloneDeep(data) {
    return JSON.parse(JSON.stringify(data));
};
