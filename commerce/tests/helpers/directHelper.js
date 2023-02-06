const directUrl = require('yandex-config').direct.url;
const nock = require('nock');

module.exports = function nockDirect() {
    nock(directUrl)
        .get(/uid=9876543210&json=1/)
        .times(Infinity)
        .reply(200, {});

    nock(directUrl)
        .get(/uid=1\d+&json=1/)
        .times(Infinity)
        .reply(200, {
            'chief_agency_login': 'i-pupkin',
            'manager_login': 'yndx-pupkin',
            role: 'agency',
            'client_id': 65432
        });
};
