const Promise = require('bluebird');
const mockery = require('mockery');

module.exports = () => {
    mockery.registerMock('../../lib/blackbox', {
        getUserInfo: () => Promise.resolve({}),
        getUsersInfo: () => Promise.resolve({})
    });
};
