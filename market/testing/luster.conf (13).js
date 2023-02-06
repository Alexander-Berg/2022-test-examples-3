const path = require('path');

module.exports = {
    // required, absolute or relative path to configuration file
    // of worker source file
    app: require.resolve('../../server.js'),

    // options to control workers startup and shutdown processes
    control: {
        // time to wait for 'online' event from worker
        // after spawning it (in milliseconds)
        forkTimeout: 3000,

        // time to wait for 'exit' event from worker
        // after disconnecting it (in milliseconds)
        stopTimeout: 1,

        // if worker dies in `exitThreshold` time (in milliseconds) after start,
        // then its' `sequentialDeaths` counter will be increased
        exitThreshold: 5000,

        // max allowed value of `sequentialDeaths` counter
        // for each worker; on exceeding this limit worker will
        // be marked as `dead` and no more automatic restarts will follow.
        allowedSequentialDeaths: 10,
    },

    // use "server" group if you want to use web workers
    server: {
        // initial port for the workers;
        // can be tcp port number or path to the unix socket;
        // if you use unix sockets with groups of the workers,
        // then path must contain '*' char, which will be replaced
        // with group number
        port: process.env.NODE_PORT || 8080,

        // number of workers' groups; each group will
        // have its own port number (port + group number)
        // groups: 2
    },

    extensions: {
        'luster-log-file': {
            // override `console.log`, `.warn`, `.info` and `.error` methods
            // to add severity marks to output
            extendConsole: true,

            // logs files, both optional
            //   {string} fileName – stream output to file
            //   true – don't redirect output, keep as is
            //   false – shut down output
            stdout: process.env.LOGS_DIR
                ? path.join(process.env.LOGS_DIR, 'output.log')
                : 'output.log',
            stderr: process.env.LOGS_DIR
                ? path.join(process.env.LOGS_DIR, 'errors.log')
                : 'errors.log',
        },
        'luster-log-winston': {
            logs: [
                {
                    name: 'redir',
                    path: process.env.LOGS_DIR
                        ? path.join(process.env.LOGS_DIR, 'sovetnik.log')
                        : 'sovetnik.log',
                },
                {
                    name: 'sovetnik-eb-errors',
                    path: process.env.LOGS_DIR
                        ? path.join(process.env.LOGS_DIR, 'sovetnik-eb-errors.log')
                        : 'sovetnik-eb-errors.log',
                    json: true,
                },
            ],
        },
        'luster-log-pid': {
            filename: path.resolve('./node-master-process.pid'),
        },
    },

    // if your app or used extensions extensively use luster
    // internal events then you can tweak internal event emitters
    // listeners number limit using following option.
    // default value is `100`, option must be a number else EventEmitter
    // throws an error on configuration.
    maxEventListeners: 100,
};
