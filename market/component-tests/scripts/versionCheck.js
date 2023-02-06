/* eslint-disable no-console */
const packageJson = require('../package.json');
const packageLockJson = require('../package-lock.json');

if (packageJson.version !== packageLockJson.version) {
    console.log(`
    Version in package.json: ${packageJson.version}
    Version in package-lock.json: ${packageLockJson.version}
    Wrong version number!`);
    throw new Error('Wrong version number!');
} else {
    console.log(`Version ${packageJson.version} OK`);
}
