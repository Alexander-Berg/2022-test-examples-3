let path = require('path');

exports.fixture = function(name) {
    return require(path.join(__dirname, 'fixtures', name + '.json'));
};
