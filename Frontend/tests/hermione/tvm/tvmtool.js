/**
 * https://wiki.yandex-team.ru/quasarui/testing-speakers/
 */

const { exec } = require('child_process');
const { TVM_TOKEN, TVM_PORT } = require('./tvm-constants');
const path = require('path');
const fs = require('fs');
const { getToken } = require('../helpers/secrets');

const tvmJsonPath = path.resolve(__dirname, '.tvm.json');

async function getTvmSecret() {
    const secretId = 'ver-01fpf5z61gh49yb7crysz9q59w';
    const secretKeyName = 'client_secret';
    return await getToken(secretId, secretKeyName);
}

async function generateTvmJson() {
    const secret = await getTvmSecret();
    const tvmJson = {
        BbEnvType: 1,
        clients: {
            'quasarui-hermione': {
                secret,
                self_tvm_id: 2032151,
                dsts: {
                    'quasar-backend': {
                        dst_id: 2002639,
                    },
                    'steelix-priemka': {
                        dst_id: 2016207,
                    },
                },
            },
        },
    };

    fs.writeFileSync(tvmJsonPath, JSON.stringify(tvmJson, null, 4));
}

async function runTvmTool() {
    const tvmtool = exec(`tvmtool --port ${TVM_PORT} --auth ${TVM_TOKEN} -c ${tvmJsonPath}`);
    tvmtool.stdout.on('data', function(data) {
        // console.log(data);
    });
}

generateTvmJson().then(() => {
    runTvmTool();
});
