const publishReports = require('./_helpers/publishReports.js');
const runTests = require('./_helpers/runTests.js');
const runPalmsync = require('./_helpers/runPalmsync.js');
const installYa = require('./_helpers/installYa.js');
const addRsaKey = require('./_helpers/addRsaKey.js');

addRsaKey();
installYa();

const areTestsSuccessful = runTests();

publishReports();

let isPalmsyncSuccessful = runPalmsync();

process.exit(Number(!(areTestsSuccessful && isPalmsyncSuccessful)));
