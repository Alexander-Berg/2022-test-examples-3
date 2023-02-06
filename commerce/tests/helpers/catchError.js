module.exports.func = function (cb) {
    try {
        cb();
    } catch (err) {
        return err;
    }

    throw new Error('Test should throw error');
};

module.exports.generator = function *(cb) {
    try {
        yield cb();
    } catch (err) {
        return err;
    }

    throw new Error('Test should throw error');
};
