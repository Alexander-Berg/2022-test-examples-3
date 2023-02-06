var path = require('path'),
    NODE_PORT = process.env.NODE_PORT,
    LOGS_DIR = process.env.LOGS_DIR,
    logs = LOGS_DIR ?
        path.resolve.bind(path, LOGS_DIR) :
        function() { return false; };

module.exports = {
    app: require.resolve('app'),

    workers: 23,

    control: {
        forkTimeout: 5000,
        stopTimeout: 10000
    },

    server: {
        port: NODE_PORT
    },

    extensions: {
        "luster-log-file": {
            extendConsole: true,
            stdout: logs('debug.log'),
            stderr: logs('error.log')
        }
    }
};
