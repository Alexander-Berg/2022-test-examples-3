const { getEnvWithSecretOr } = require('@yandex-int/si.ci.env-utils');

const TVM_PORT = 3031;
const TVM_TOKEN = getEnvWithSecretOr('TVMTOOL_LOCAL_AUTHTOKEN', 'tvmtool-development-access-token');

module.exports = {
    TVM_PORT,
    TVM_TOKEN,
};
