const config = require('yandex-config').proctoring;
const nock = require('nock');

module.exports = {
    protocol(data) {
        return nock(config.host)
            .get(`${config.protocolPath}/${data.openId}`)
            .times(Infinity)
            .reply(data.code || 200, data.response);
    },

    retry(data) {
        return nock(config.host)
            .get(`${config.protocolPath}/${data.openId}`)
            .once()
            .reply(data.code)
            .get(`${config.protocolPath}/${data.openId}`)
            .reply(200, data.response);
    },

    events(data) {
        return nock(config.host)
            .get(`${config.eventsPath}/${data.openId}`)
            .times(Infinity)
            .reply(data.code || 200, data.response);
    },

    retryEvents(data) {
        return nock(config.host)
            .get(`${config.eventsPath}/${data.openId}`)
            .once()
            .reply(data.code)
            .get(`${config.eventsPath}/${data.openId}`)
            .reply(200, data.response);
    },

    file(data) {
        return nock(config.host)
            .get(`${config.storagePath}/${data.id}`)
            .times(Infinity)
            .reply(data.code || 200, data.response);
    },

    retryFile(data) {
        return nock(config.host)
            .get(`${config.storagePath}/${data.id}`)
            .once()
            .reply(data.code)
            .get(`${config.storagePath}/${data.id}`)
            .reply(200, data.response);
    },

    user(data) {
        return nock(config.host)
            .get(`${config.userPath}/${data.login}`)
            .times(Infinity)
            .reply(data.code || 200, data.response);
    },

    retryUser(data) {
        return nock(config.host)
            .get(`${config.userPath}/${data.login}`)
            .once()
            .reply(data.code)
            .get(`${config.userPath}/${data.login}`)
            .reply(200, data.response);
    }
};
