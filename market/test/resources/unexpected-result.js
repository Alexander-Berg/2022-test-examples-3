const Resource = require('./setup');

const ResourceError = Resource.Error;
const ResourceUnexpectedResult = Resource.create();

ResourceUnexpectedResult.prototype.get = function () {
    throw ResourceError.createError(ResourceError.CODES.UNEXPECTED_RESULT, {
        url: 'example.com',
        results: JSON.stringify({some: 'trololo'}),
    });
};

module.exports = ResourceUnexpectedResult;
