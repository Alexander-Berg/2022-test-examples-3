const nock = require('nock');
const config = require('cfg');

function nockMyCertificates() {
    nock(config.api.host)
        .get(`/${config.api.version}/certificates/my`)
        .query(true)
        .reply(401);
}

function nockExamDirect() {
    nock(config.api.host)
        .get(`/${config.api.version}/exam/direct`)
        .reply(200, { id: 1, title: 'Сертификация' });
}

function nockAttemptDirectCheck() {
    nock(config.api.host)
        .get(`/${config.api.version}/attempt/direct/check`)
        .reply(200, { state: 'enabled', firstname: 'bloguser' });
}

function nockExamCluster(slug) {
    nock(config.api.host)
        .get(`/${config.api.version}/exam/cluster/${slug}`)
        .reply(404);
}

function cleanAll() {
    nock.cleanAll();
}

module.exports = {
    nockMyCertificates,
    nockExamDirect,
    nockAttemptDirectCheck,
    nockExamCluster,
    cleanAll
};
