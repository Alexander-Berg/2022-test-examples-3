const getUser = require('@ps-int/ufo-hermione/helpers/getUser')({});

module.exports = function (login) {
    return this.loginFast(typeof login === 'string' ? getUser(login) : login);
};
