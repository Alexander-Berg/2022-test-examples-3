const path = require('path');
const winston = require('winston');
const luster = require('luster');
const { removeEmptyOrNull } = require('./clear-object-empties');

const PROJECT_NAME = 'SOVETNIK_REDIR';
const SERVICE_NAME = 'SOVETNIK';
const PROJECT_LANGUAGE = 'nodejs';

const { getLogsFromQuery, prepareLogData } = require('../helpers/log-functions');

const logFiles = {
    errorBooster: {
        loggerWinston: getLogger('sovetnik-eb-errors.log', 'sovetnik-eb-errors', true),
    },
};

function getTransport(logFileName, jsonFormat = false) {
    return new winston.transports.File({
        format: jsonFormat
            ? winston.format.json()
            : winston.format.printf((info) => `${info.message}`),
        level: 'info',
        filename: process.env.LOGS_DIR ? path.join(process.env.LOGS_DIR, logFileName) : logFileName,
    });
}

function getLogger(logFileName, logName, jsonFormat = false) {
    if (!logFileName) {
        throw new Error('logFileName is not defined');
    }
    if (!logName) {
        throw new Error('logName is not defined');
    }

    if (luster && luster.logWinston && luster.logWinston[logName]) {
        return luster.logWinston[logName];
    }
    return winston.createLogger({
        levels: winston.config.npm.levels,
        exitOnError: false,
        transports: [getTransport(logFileName, jsonFormat)],
    });
}

function noticeError(error, req) {
    let logs = prepareLogData(req);

    logs.timestamp = Date.now();

    logs.service = SERVICE_NAME;
    logs.project = PROJECT_NAME;
    logs.language = PROJECT_LANGUAGE;

    if (error.name) {
        logs.message = error.name;
    }

    if (error.message) {
        logs.additional = { message: error.message };
    }

    if (error.stack) {
        logs.stack = error.stack;
    }

    const additionalInfo = getLogsFromQuery(req.query);
    logs.additional = Object.assign(logs.additional, additionalInfo);

    logs = removeEmptyOrNull(logs);

    logFiles.errorBooster.loggerWinston.info(logs);
}
module.exports = { getLogger, noticeError };
