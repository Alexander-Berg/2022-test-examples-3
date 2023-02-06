module.exports = function (cb) {
    try {
        cb(); // eslint-disable-line callback-return
    } catch (err) {
        return err;
    }

    throw new Error('Test should throw an error');
};
