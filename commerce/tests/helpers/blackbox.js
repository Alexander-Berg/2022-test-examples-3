const _ = require('lodash');
const config = require('yandex-config');
const nock = require('nock');

function nockBlackbox(BBConfig, options) {
    nock(`http://${BBConfig.connection.api}`)
        .get('/blackbox')
        .query(_.assign({
            method: 'sessionid',
            format: 'json',
            sessionid: _.get(options, 'request.sessionid', 'user_session_id'),
            sslsessionid: '',
            host: '127.0.0.1',
            userip: '::ffff:127.0.0.1',
            attributes: BBConfig.attributes,
            emails: 'getdefault'
        }, options.params))
        .times(Infinity)
        .reply(200, options.response || {});

    nock(`http://${BBConfig.connection.api}`)
        .get('/blackbox')
        .query({
            method: 'userinfo',
            format: 'json',
            uid: 1029384756,
            userip: '::ffff:127.0.0.1',
            attributes: BBConfig.attributes
        })
        .times(Infinity)
        .reply(200, {
            users: [
                {
                    uid: { value: 1029384756 },
                    attributes: {
                        27: 'Ivan',
                        28: 'Ivanov'
                    }
                }
            ]
        });

    nock(`http://${BBConfig.connection.api}`)
        .get('/blackbox')
        .query({
            method: 'userinfo',
            format: 'json',
            uid: 1111111111111111,
            userip: '::ffff:127.0.0.1',
            attributes: BBConfig.attributes
        })
        .times(Infinity)
        .reply(200, {});
}

function nockSeveralUids(BBConfig, options) {
    nock(`http://${BBConfig.connection.api}`)
        .get('/blackbox')
        .query({
            method: 'userinfo',
            format: 'json',
            uid: options.uid,
            userip: options.userip,
            attributes: BBConfig.attributes,
            emails: 'getdefault'
        })
        .times(Infinity)
        .reply(200, options.response);
}

module.exports = {
    nockExtBlackbox: nockBlackbox.bind(null, config.blackbox),

    nockIntBlackbox: nockBlackbox.bind(null, config.yandexTeamBlackbox),

    nockExtSeveralUids: nockSeveralUids.bind(null, config.blackbox),

    cleanAll: nock.cleanAll
};
