const path = require('path');

const lusterConf = {
    // required, absolute or relative path to configuration file
    // of worker source file
    app: require.resolve('../../server.js'),

    // workers number
    // number of cpu threads is used by default
    //workers: 2,

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
        //groups: 2
    },

    //debug: {
    //    // debug port for first worker; each following will
    //    // use previous worker port + 1
    //    port: 5010
    //},

    // extensions to load
    // each key in the "extensions" hash is a npm module name
    extensions: {
        'luster-log-pid': {
            filename: path.resolve('./node-master-process.pid'),
        },
        'luster-log-file': {
            // override `console.log`, `.warn`, `.info` and `.error` methods
            // to add severity marks to output
            extendConsole: true,

            // logs files, both optional
            //   {string} fileName – stream output to file
            //   true – don't redirect output, keep as is
            //   false – shut down output
            stdout: process.env.LOGS_DIR ? path.join(process.env.LOGS_DIR, 'output.log') : 'output.log',
            stderr: process.env.LOGS_DIR ? path.join(process.env.LOGS_DIR, 'errors.log') : 'errors.log',
        },
        'luster-log-winston': {
            logs: [
                {
                    name: 'product',
                    path: process.env.LOGS_DIR ? path.join(process.env.LOGS_DIR, 'sovetnik.log') : 'sovetnik.log',
                },
                {
                    name: 'avia',
                    path: process.env.LOGS_DIR
                        ? path.join(process.env.LOGS_DIR, 'sovetnik-avia.log')
                        : 'sovetnik-avia.log',
                },
                {
                    name: 'settings',
                    path: process.env.LOGS_DIR
                        ? path.join(process.env.LOGS_DIR, 'sovetnik-settings.log')
                        : 'sovetnik-settings.log',
                },
                {
                    name: 'externalApi',
                    path: process.env.LOGS_DIR
                        ? path.join(process.env.LOGS_DIR, 'sovetnik-external-api.log')
                        : 'sovetnik-external-api.log',
                },
                {
                    name: 'disableReason',
                    path: process.env.LOGS_DIR
                        ? path.join(process.env.LOGS_DIR, 'sovetnik-disable-reason.log')
                        : 'sovetnik-disable-reason.log',
                },
                {
                    name: 'clientEvent',
                    path: process.env.LOGS_DIR
                        ? path.join(process.env.LOGS_DIR, 'sovetnik-client-event.log')
                        : 'sovetnik-client-event.log',
                },
                {
                    name: 'domainData',
                    path: process.env.LOGS_DIR
                        ? path.join(process.env.LOGS_DIR, 'sovetnik-domain-data.log')
                        : 'sovetnik-domain-data.log',
                },
                {
                    name: 'users',
                    path: process.env.LOGS_DIR
                        ? path.join(process.env.LOGS_DIR, 'sovetnik-users.log')
                        : 'sovetnik-users.log',
                },
                {
                    name: 'buy',
                    path: process.env.LOGS_DIR
                        ? path.join(process.env.LOGS_DIR, 'sovetnik-buy.log')
                        : 'sovetnik-buy.log',
                },
                {
                    name: 'sovetnik-partner-external-requests',
                    path: process.env.LOGS_DIR
                        ? path.join(process.env.LOGS_DIR, 'sovetnik-partner-external-requests.log')
                        : 'sovetnik-partner-external-requests.log',
                },
                {
                    name: 'reports',
                    path: process.env.LOGS_DIR
                        ? path.join(process.env.LOGS_DIR, 'sovetnik-reports.log')
                        : 'sovetnik-reports.log',
                },
                {
                    name: 'offerUrls',
                    path: process.env.LOGS_DIR
                        ? path.join(process.env.LOGS_DIR, 'sovetnik-offer-urls.log')
                        : 'sovetnik-offer-urls.log',
                },
                {
                    name: 'sovetnik-wishlist',
                    path: process.env.LOGS_DIR
                        ? path.join(process.env.LOGS_DIR, 'sovetnik-wishlist.log')
                        : 'sovetnik-wishlist.log',
                },
                {
                    name: 'models-metrics',
                    path: process.env.LOGS_DIR
                        ? path.join(process.env.LOGS_DIR, 'sovetnik-models-metrics.log')
                        : 'sovetnik-models-metrics.log',
                },
                {
                    name: 'init',
                    path: process.env.LOGS_DIR
                        ? path.join(process.env.LOGS_DIR, 'sovetnik-init.log')
                        : 'sovetnik-init.log',
                },
                {
                    name: 'auto',
                    path: process.env.LOGS_DIR
                        ? path.join(process.env.LOGS_DIR, 'sovetnik-auto.log')
                        : 'sovetnik-auto.log',
                },
                {
                    name: 'add-to-cart',
                    path: process.env.LOGS_DIR
                        ? path.join(process.env.LOGS_DIR, 'sovetnik-add-to-cart.log')
                        : 'sovetnik-add-to-cart.log',
                },
                {
                    name: 'sovetnik-pp',
                    path: process.env.LOGS_DIR ? path.join(process.env.LOGS_DIR, 'sovetnik-pp.log') : 'sovetnik-pp.log',
                },
                {
                    name: 'sovetnik-eb-errors',
                    path: process.env.LOGS_DIR
                        ? path.join(process.env.LOGS_DIR, 'sovetnik-eb-errors.log')
                        : 'sovetnik-eb-errors.log',
                    json: true,
                },
                {
                    name: 'sovetnik-adfox',
                    path: process.env.LOGS_DIR
                        ? path.join(process.env.LOGS_DIR, 'sovetnik-adfox.log')
                        : 'sovetnik-adfox.log',
                    json: true,
                },
            ],
        },
    },

    // if extensions' modules can't be resolved as related to
    // luster module or worker path, then absolute path
    // to the directory, which contains extensions modules
    // must be declared here:
    //extensionsPath: "/usr/local/luster-extensions",

    // if your app or used extensions extensively use luster
    // internal events then you can tweak internal event emitters
    // listeners number limit using following option.
    // default value is `100`, option must be a number else EventEmitter
    // throws an error on configuration.
    maxEventListeners: 100,
};

// number of cpu threads is used by default
if (+process.env.NODE_WORKERS) {
    lusterConf.workers = +process.env.NODE_WORKERS;
}

module.exports = lusterConf;
