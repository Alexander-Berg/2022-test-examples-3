module.exports = function binaryParser(res, callback) {
    res.setEncoding('binary');

    res.data = '';

    res.on('data', chunk => {
        res.data += chunk;
    });
    res.on('end', () => callback(null, Buffer.from(res.data, 'binary')));
};
