let util = require('util');
let sinon = require('sinon');

module.exports = function() {
    return {
        apply: sinon.spy(function(data) {
            if (data.block === 'error') {
                throw new Error('bh err');
            }

            return util.format('<%s></%s>', data.block, data.block);
        }),
        setData: sinon.stub(),
    };
};
