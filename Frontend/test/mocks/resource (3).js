var sinon = require('sinon'),
    Vow = require('vow'),
    response = {
        foo: 'bar',
        someparam: 42,
        cat: 'meow'
    };

module.exports = function() {
    return {
        response: response,
        callMethod: sinon.stub().returns(Vow.resolve(response))
    };
};
