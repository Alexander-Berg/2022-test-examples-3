const publicLogin = require('../../../../disk-public/tests/hermione/config').login;
const wwwLogin = require('../../hermione/config').login;
const getUser = require('@ps-int/ufo-hermione/helpers/getUser')({});

// часть пользователей в users нужна для регрессионных тест-кейсов, эти аккаунты не используются для автотестов
// см. таблицу https://wiki.yandex-team.ru/users/viktormarushc/akkaunty-dlja-testirovanija-overdraftov/"
const overdraftUsers = require('./overdraft-users').reduce((result, item) => {
    result[item.user] = item.user.startsWith('yndx-ufo-test-') ?
        getUser(item.user) :
        {
            login: item.user,
            password: 'testiwan'
        };
    return result;
}, {});

module.exports = Object.assign({}, publicLogin, wwwLogin, overdraftUsers);
