const config = require('config');
const url = require('url');
const querystring = require('querystring');

const mergeQueryParams = (_url, qp) => {
    const parsedUrl = url.parse(_url, true);
    Object.entries(qp).forEach(([key, value]) => {
        parsedUrl[key] = value;
    });
    parsedUrl.query = { ...parsedUrl.query, ...qp };
    parsedUrl.search = querystring.stringify(parsedUrl.query);
    return url.format(parsedUrl);
};

const withFakeLogin = (_url, login = config.sbsRobotLogin) => {
    return mergeQueryParams(_url, { 'fake-login': login });
};

const withFakeLoginAndDbError = (_url, login = config.sbsRobotLogin, error = true) => {
    return mergeQueryParams(_url, { 'fake-login': login, 'fake-db-error': error });
};

module.exports = { withFakeLogin, withFakeLoginAndDbError };
