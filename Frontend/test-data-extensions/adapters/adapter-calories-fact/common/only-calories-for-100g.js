var dataStub = require('../data')();

dataStub.construct.data.size = [];
dataStub.construct.data.attributes = [];

module.exports = {
    type: 'snippet',
    data_stub: dataStub
};
