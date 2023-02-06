const ChildProcess = require('child_process');
const Fs = require('fs');

const paths = require('./paths');

const silent = process.env.SILENT;

module.exports = function build(config, environment) {
    try {
        Fs.lstatSync(paths.results);
    } catch (e) {
        Fs.mkdirSync(paths.results);
    }

    ChildProcess.execSync(`${paths.webpackBin} --config ${config}`, {
        stdio: silent ? 'ignore' : 'inherit',
        cwd: paths.filesRoot,
        env: {
            ...process.env,
            DISABLE_CACHE: 1,
            NODE_ENV: 'production',
            BUILD_ENV: environment,
        },
    });
};
