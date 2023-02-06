const Resource = require('./setup');

const ResourceError = Resource.Error;
const ResourceInvalidArguments = Resource.create();

ResourceInvalidArguments.prototype.get = function () {
    throw ResourceError.createError(ResourceError.CODES.INVALID_ARGUMENTS, {
        url: 'example.com',
    });
};

module.exports = ResourceInvalidArguments;
