const config = require('yandex-cfg');
const nock = require('nock');

module.exports = () => {
    nock(config.tvm.serverUrl)
        .get(/tickets/)
        .times(Infinity)
        .reply(200, {
            blogs: {
                ticket: '123',
                'tvm_id': 2000081
            },
            direct: {
                ticket: '123',
                'tvm_id': 2000693
            }
        });
};
