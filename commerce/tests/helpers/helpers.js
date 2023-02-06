'use strict';

const path = require('path');
const url = require('url');
const request = require('../../request');
const passport = require('cfg').passport;

require('chai').should();
process.env.NODE_ENV = process.env.NODE_ENV || require('yandex-environment') || 'development';
process.env.CFG_DIR = path.join(__dirname, '..', '..', 'configs');

var passportUrl = url.format({
    protocol: passport.protocol,
    host: passport.host('ru'),
    pathname: passport.paths.auth
});

module.exports = {
    getSessionIdOfUser: function (login, password) {
        // POST запрос в паспорт для авторизации пользователяы
        return request(passportUrl, {
            body: {
                login: login,
                passwd: password,
                timestamp: Date.now(),
                twoweeks: 1
            }
        })

        // Поиск в ответе куки `Session_id`
            .catch(function (error) {
                var res = error.response;
                res.statusCode.should.equal(302);
                var setCookie = res.headers['set-cookie'];

                // Поиск среди устанавливаемых кук записи содержащей `Session_id`
                return setCookie.reduce((result, cookie) => {
                    return cookie.indexOf('Session_id') >= 0 ? cookie.split(';')[0] : result;
                }, '');
            });
    }
};
