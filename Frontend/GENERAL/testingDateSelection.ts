/* eslint-disable */
import { SkillInstance } from '../../../db/tables/skill';
import { getTicket } from '../../startrek';
import { Channel } from '../../../db/tables/settings';
import config from '../../config';
import { DevicesTestingRecord } from '../../../db';
import { encryptAES } from '../../crypto/aes';
import { serializeDevicesTestingRecord } from '../../../serializers/devicesTestingRecord';
import {
    DevicesTestingRecordType,
    DevicesTestingRecordOptions,
} from '../../../db/tables/devicesTestingRecord';

class InvalidSkillError extends Error {
    constructor() {
        super('Invalid skill channel or draft status or moderation state');
    }
}

export enum TestingDateStatus {
    APPROVED = 'APPROVED',
    REJECTED = 'REJECTED',
    AWAITING = 'AWAITING',
    PASSED = 'PASSED',
    NOT_SPECIFIED = 'NOT_SPECIFIED',
}

export const checkTestingDateSelectionStatus = async(
    skill: SkillInstance,
): Promise<TestingDateStatus> => {
    if (skill.channel !== Channel.SmartHome || !skill.draft.startrekTicket) {
        throw new InvalidSkillError();
    }

    const ticket = await getTicket(skill.draft.startrekTicket);

    switch (ticket.status.key) {
        case 'meetingApproval':
            return (ticket.tags ?? []).includes(config.startrek.smartHomeInvalidTestDateTag) ?
                TestingDateStatus.REJECTED :
                TestingDateStatus.AWAITING;
        case 'waitingForTesting':
            return TestingDateStatus.APPROVED;
        default:
            return TestingDateStatus.PASSED;
    }
};

interface CreateDevicesTestingRecordParams {
    type: DevicesTestingRecordType;
    skillId: string;
    options?: DevicesTestingRecordOptions;
}

export const createOrUpdateDevicesTestingRecord = async({
    skillId,
    type,
    options,
}: CreateDevicesTestingRecordParams) => {
    if (options) {
        options.password = encryptAES(options.password);
    }

    const [updatedCount, [updatedInstance]] = await DevicesTestingRecord.update(
        {
            type,
            options: options ?? null,
        },
        {
            where: {
                skillId,
            },
            returning: true,
        },
    );

    if (updatedCount === 0) {
        return await DevicesTestingRecord.create({
            skillId,
            type,
            options: options ?? null,
        });
    }

    return updatedInstance;
};

interface RemoveDevicesTestingRecordParams {
    skillId: string;
}

export const removeDevicesTestingRecord = async({ skillId }: RemoveDevicesTestingRecordParams) => {
    return DevicesTestingRecord.destroy({
        where: { skillId },
    });
};

interface GetDevicesTestingRecordParams {
    skillId: string;
}

export const getDevicesTestingRecord = async({ skillId }: GetDevicesTestingRecordParams) => {
    const record = await DevicesTestingRecord.findOne({
        where: {
            skillId,
        },
    });

    return record ? serializeDevicesTestingRecord(record) : null;
};
