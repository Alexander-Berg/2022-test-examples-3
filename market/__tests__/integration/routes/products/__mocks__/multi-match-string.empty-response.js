import fs from 'fs';
import path from 'path';

const HOST = 'http://cs-matcher-api.http.yandex.net:34513';
const ROUTE = /\/MultiMatchString/i;

const RESPONSE = fs.readFileSync(path.join(__dirname, 'MatcherStringResponse.bin'), null);

module.exports = {
    host: HOST,
    route: ROUTE,
    response: RESPONSE,
    method: 'post',
};
