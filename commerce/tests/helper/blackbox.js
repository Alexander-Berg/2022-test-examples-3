const mockery = require('mockery');

module.exports = (options = {}) => {
    mockery.registerMock('express-blackbox', () =>
        (req, res, next) => {
            req.blackbox = options;
            next();
        }
    );
};
