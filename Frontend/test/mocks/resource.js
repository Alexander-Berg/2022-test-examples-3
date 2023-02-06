let sinon = require('sinon');
let Vow = require('vow');
let response = {
    foo: 'bar',
    someparam: 42,
    cat: 'meow',
};

module.exports = function() {
    return {
        response: response,
        callMethod: sinon.stub().returns(Vow.resolve(response)),
    };
};
