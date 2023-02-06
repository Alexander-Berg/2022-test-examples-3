const nock = require('nock');
const config = require('yandex-config');

module.exports = {
    checkTicket: function nockTvmTicketCheck(res, code) {
        code = code || 200;

        return nock(config.tvm.host)
            .get('/checksrv')
            .query(true)
            .reply(code, res);
    },

    getTicket: function nockGetTicket(reply, times) {
        times = times || 1;

        return nock(config.tvm.host)
            .get('/tickets')
            .query(true)
            .times(times)
            .reply(200, reply);
    }
};
