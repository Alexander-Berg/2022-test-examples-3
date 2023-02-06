'use strict';

/**
 *  Удаление строк скип-пака по закрытым тикетам.
 *
 *  Для получения OAuth-токена ST зайдите из под нужного пользователя на
 *  https://oauth.yandex-team.ru/authorize?response_type=token&client_id=5f671d781aca402ab7460fde4050267b
 *
 *  Для получения OAuth-токена TestPalm зайдите из под нужного пользователя на
 *  https://oauth.yandex-team.ru/authorize?response_type=token&client_id=6d967b191847496a8a7077e2e636142f
 */

const TOKEN_FILE = 'oauth_token.txt';
const ST_OAUTH_TOKEN_REG_EXP = /ST_OAUTH_TOKEN=([\w-]+)/;
const TESTPALM_OAUTH_API_TOKEN_REG_EXP = /TESTPALM_OAUTH_API_TOKEN=([\w-]+)/;
const {readFileSync} = require('fs');
const {relative, join} = require('path');

function readStOAuthToken() {
    return readOAuthToken(ST_OAUTH_TOKEN_REG_EXP);
}

function readTestPalmOAuthToken() {
    return readOAuthToken(TESTPALM_OAUTH_API_TOKEN_REG_EXP);
}

function readOAuthToken(regExp) {
    try {
        const path = relative('.', join(__dirname, TOKEN_FILE));
        const file = readFileSync(path, {encoding: 'utf-8'});
        return file.match(regExp)[1];
    } catch (e) {
        e.message = `[${TOKEN_FILE}] ${e.message}`;
        throw e;
    }
}

module.exports = {
    readStOAuthToken,
    readTestPalmOAuthToken,
};
