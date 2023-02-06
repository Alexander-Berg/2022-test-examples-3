/* eslint-disable */
import test from 'ava';
import { encryptAES, decryptAES } from '../../../services/crypto/aes';
import config from '../../../services/config';

const message = 'Some long message containing several blocks';
const encryptedMessage = 'K4Lnkr259pDBFwboL1WzMx9LHadc+qUtUdZCiMrP0isIWMyf1+Avg9aPi0Mmf/sa';

test.before(() => {
    config.app.secretKey = 'abcdefghijklmnop';
});

test.after(() => {
    config.app.secretKey = '';
});

test('encryptAES: encrypting similar data to similar result', t => {
    const enc1 = encryptAES(message);
    const enc2 = encryptAES(message);

    t.is(enc1, enc2);
});

test('encryptAES: encrypting correctly', t => {
    t.is(encryptedMessage, encryptAES(message));
});

test('decryptAES: decrypting correctly', t => {
    t.is(message, decryptAES(encryptedMessage));
});
