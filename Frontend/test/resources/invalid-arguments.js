let Resource = require('./setup');
let ResourceError = Resource.Error;
let ResourceInvalidArguments = Resource.create();

ResourceInvalidArguments.prototype.get = function() {
    throw ResourceError.createError(ResourceError.CODES.INVALID_ARGUMENTS, {
        url: 'example.com',
    });
};

module.exports = ResourceInvalidArguments;
