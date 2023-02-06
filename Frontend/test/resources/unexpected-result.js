let Resource = require('./setup');
let ResourceError = Resource.Error;
let ResourceUnexpectedResult = Resource.create();

ResourceUnexpectedResult.prototype.get = function() {
    throw ResourceError.createError(ResourceError.CODES.UNEXPECTED_RESULT, {
        url: 'example.com',
        results: JSON.stringify({ some: 'trololo' }),
    });
};

module.exports = ResourceUnexpectedResult;
