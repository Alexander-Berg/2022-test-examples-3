const BACKEND_SERVICE_TICKET = '3:serv:CL-nARCzxbGTBiIRCOuqexDBl3wg5qCZkaXU_gE:*';
const INTAPI_SERVICE_TICKET = '3:serv:CIeoARDortKTBiIRCMGXfBC8iXog5qCZkaXU_gE:*';

jest.mock('../../server/lib/helpers/tvm', () => ({
    checkService(core, selfTvmId, ticket) {
        if (ticket === BACKEND_SERVICE_TICKET) {
            return Promise.resolve({ src: 2020715 });
        } else {
            return Promise.resolve({});
        }
    },

    getTickets() {
        return Promise.resolve({
            intapi: INTAPI_SERVICE_TICKET
        });
    }
}));

module.exports = {
    BACKEND_SERVICE_TICKET,
    INTAPI_SERVICE_TICKET
};
