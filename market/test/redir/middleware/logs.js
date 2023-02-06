const { getLogger } = require('../utils/winston-logs');
const { makeLogString, getLogsFromQuery, prepareLogData } = require('../helpers/log-functions');

const logFiles = {
    redir: {
        begin: 'tskv\ttskv_format=sovetnik-redir-log\t',
        loggerWinston: getLogger('sovetnik.log', 'redir'),
    },
};

function logMiddleware(req, res, next) {
    let logs = prepareLogData(req);

    logs.tst = 'CSADMIN-19278';

    logs = Object.assign(logs, getLogsFromQuery(req.query));

    let logString = logFiles.redir.begin;

    logString += makeLogString(logs);

    logFiles.redir.loggerWinston.info(logString);

    next();
}

module.exports = logMiddleware;
