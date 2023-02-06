import fs from 'fs';
import path from 'path';

const HOST = 'http://cs-matcher-api.http.yandex.net:34513';
const ROUTE = /\/MultiMatch/i;

const RESPONSE = fs.readFileSync(path.join(__dirname, 'MatcherResponse.bin'), null);

module.exports = {
    host: HOST,
    route: ROUTE,
    response: RESPONSE,
    method: 'post',
};
