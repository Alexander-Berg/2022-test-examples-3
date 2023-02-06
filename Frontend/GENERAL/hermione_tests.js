const publishReports = require('./_helpers/publishReports.js');
const runTests = require('./_helpers/runTests.js');
const installYa = require('./_helpers/installYa.js');
const addRsaKey = require('./_helpers/addRsaKey.js');

addRsaKey();
installYa();

const areTestsSuccessful = runTests();

publishReports();

process.exit(Number(!areTestsSuccessful));
