import {
    EActionParamTypeDto,
    ETestCaseSnapshotStatusDto,
    ETestCaseStatusDto,
    IThiriumLaunchTestsRequest,
} from 'src/services/thirium/types';

export enum EThiriumConfigPreset {
    RASP_TESTING = 'RASP_TESTING',
}

export function isThiriumConfigPreset(
    candidate: unknown,
): candidate is EThiriumConfigPreset {
    return Object.values(EThiriumConfigPreset).some(v => v === candidate);
}

const RASP_TESTING: IThiriumLaunchTestsRequest = {
    projects: ['RASP'],
    testCaseStatuses: [ETestCaseStatusDto.READY],
    snapshotStatuses: [
        ETestCaseSnapshotStatusDto.DIRTY,
        ETestCaseSnapshotStatusDto.APPROVED,
    ],
    tags: ['smoke'],
    simultaneousRunsLimit: 20,
    maxTryCount: 3,
    params: {
        test_stand: {
            optionId: 'test_stand',
            type: EActionParamTypeDto.CONSTANT,
            value: 'https://rasp.crowdtest.yandex.ru',
        },
        test_stand_touch: {
            optionId: 'test_stand_touch',
            type: EActionParamTypeDto.CONSTANT,
            value: 'https://t.rasp.crowdtest.yandex.ru',
        },
        'capabilities.browserName': {
            optionId: 'capabilities.browserName',
            type: EActionParamTypeDto.CONSTANT,
            value: 'chrome',
        },
        'capabilities.browserVersion': {
            optionId: 'capabilities.browserVersion',
            type: EActionParamTypeDto.CONSTANT,
            value: '90.0',
        },
    },
};

export default {[EThiriumConfigPreset.RASP_TESTING]: RASP_TESTING};
