import {Status as TestStatus} from '@jest/test-result';
import {Status as AllureStatus} from 'allure2-js-commons';

const getAllureStatus = (testStatus?: TestStatus): AllureStatus => {
    switch (testStatus) {
        case 'todo':
        case 'pending':
        case 'skipped':
        case 'disabled':
            return AllureStatus.SKIPPED;
        case 'passed':
            return AllureStatus.PASSED;
        case 'failed':
            return AllureStatus.FAILED;
        default:
            return AllureStatus.BROKEN;
    }
};

export default getAllureStatus;
