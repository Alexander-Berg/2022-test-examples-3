const tvm = require('@yandex-int/tvm');
const { TVM_TOKEN, TVM_PORT } = require('../tvm/tvm-constants');

const tvmClient = new tvm.Client({
    daemonBaseUrl: 'http://localhost:' + TVM_PORT,
    token: TVM_TOKEN,
});

const getTvmServiceTicket = async(serviceName) => {
    return await tvmClient.getServiceTicket(serviceName);
};

module.exports = {
    getTvmServiceTicket,
};
