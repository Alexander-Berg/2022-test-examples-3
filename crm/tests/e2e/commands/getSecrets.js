const { VaultClient } = require('@yandex-int/vault-client');

let secretPromise = null;

//параметры функции:
//токен робота
//id секрета
module.exports = async function getSecrets(yavRobotToken, secret) {
  const client = new VaultClient(yavRobotToken);
  const prevNodeTlsRejectUauthorized = process.env.NODE_TLS_REJECT_UNAUTHORIZED;

  if (!secretPromise) {
    //NODE_TLS_REJECT_UNAUTHORIZED=0 отключает проверку ssl-сертификатов
    process.env.NODE_TLS_REJECT_UNAUTHORIZED = 0;
    secretPromise = await client.getVersion(secret);
    process.env.NODE_TLS_REJECT_UNAUTHORIZED = prevNodeTlsRejectUauthorized;
  }
  //если его уже однажды запросили, то оставить предыдущий результат
  const token = await secretPromise;

  //в секрете только одно значение, поэтому берем его
  const { value: password } = token[0];
  return password;
};
