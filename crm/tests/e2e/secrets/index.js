const path = require('path');
const fs = require('fs');
const { VaultClient } = require('@yandex-int/vault-client');

const secretOdissey = { key: 'sec-01ek232vrzr6r0wv0vpq295xy5', robot: 'odissey' }; // robot-space-odissey-cookie
const secretX = { key: 'sec-01f9c9yvgcq34zccbnqhwh1mky', robot: 'x' }; // robot-space-x-cookie

const yavRobotToken = process.argv[2];

async function getSecrets(secret) {
  const client = new VaultClient(yavRobotToken);

  const prevNodeTlsRejectUauthorized = process.env.NODE_TLS_REJECT_UNAUTHORIZED;

  process.env.NODE_TLS_REJECT_UNAUTHORIZED = 0;
  const data = await client.getVersion(secret.key);
  process.env.NODE_TLS_REJECT_UNAUTHORIZED = prevNodeTlsRejectUauthorized;

  const cookie = {};

  data.forEach((item) => {
    if (item.key === 'expiry') cookie[item.key] = parseInt(item.value);
    else if (item.value.toLowerCase() === 'true') cookie[item.key] = true;
    else cookie[item.key] = item.value;
  });

  fs.writeFileSync(path.resolve(`cookies_${secret.robot}.json`), JSON.stringify(cookie), 'utf8');
}

getSecrets(secretOdissey);
getSecrets(secretX);
