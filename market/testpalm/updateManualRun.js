const {updateRun, getPlatformSkips} = require('./utils');
const {MANUAL_RUN_ID, PROJECT_ID} = require('./constants');

const projectId = process.env.TESTPALM_PROJECT_ID || PROJECT_ID;
const testCaseId = process.env.TESTPALM_TESTCASE_ID || MANUAL_RUN_ID;

const updateManualRun = async () => {
    const skips = getPlatformSkips();

    return updateRun(projectId, testCaseId, skips);
};

updateManualRun();
