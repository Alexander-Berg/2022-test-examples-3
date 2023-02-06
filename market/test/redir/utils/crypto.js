/**
 * Any changes in this module should be consistent with same module at backend repository: utils/crypto.js
 */

require('./../env');
const crypto = require('crypto');

const password = process.env.SOVETNIK_CRYPTO || process.env.CRYPTO;
const algorithm = 'aes256';

function encrypt(string) {
    var cipher = crypto.createCipher(algorithm, password);
    return cipher.update(string, 'utf8', 'hex') + cipher.final('hex');
}

function decrypt(encrypted) {
    var decipher = crypto.createDecipher(algorithm, password);
    return decipher.update(encrypted, 'hex', 'utf8') + decipher.final('utf8');
}

module.exports.encrypt = encrypt;
module.exports.decrypt = decrypt;
