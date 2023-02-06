/* eslint-disable no-console */

const chalk = require('chalk');

const divider = chalk.gray('\n-----------------------------------');

const LEVELS = [chalk.gray, chalk.red, chalk.yellow, chalk.blue, chalk.greenBright, chalk.bgBlueBright];

/**
 * Logger middleware, you can customize it to make messages more personal
 */
const logger = {
    log: (lvl, message) => {
        const printLevel = LEVELS[lvl] || chalk.red;
        console.log(printLevel(message));
    },

    // Called whenever there's an error on the server we want to print
    error: err => {
        console.error(chalk.red(err));
    },

    // Called when express.js app starts on given port w/o errors
    appStarted: (port, host, tunnelStarted) => {
        console.log(`Server started ! ${chalk.green('✓')}`);

        // If the tunnel started, log that and the URL it's available at
        if (tunnelStarted) {
            console.log(`Tunnel initialised ${chalk.green('✓')}`);
        }

        console.log(`
${chalk.bold('Access URLs:')}${divider}
Localhost: ${chalk.magenta(`http://${host}:${port}`)}
${chalk.blue(`Press ${chalk.italic('CTRL-C')} to stop`)}
    `);
    },
};

module.exports = logger;
