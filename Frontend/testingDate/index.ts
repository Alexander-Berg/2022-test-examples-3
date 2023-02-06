import { asyncJsonResponse } from '../../../../api/rest/utils';
import {
    getDevicesTestingRecord,
    createOrUpdateDevicesTestingRecord,
    checkTestingDateSelectionStatus,
    TestingDateStatus,
} from '../../../../services/moderation/smart-home/testingDateSelection';
import { findUserSkillWithId } from '../../../../db/entities';
import { serializeDevicesTestingRecord } from '../../../../serializers/devicesTestingRecord';
import RestApiError from '../../../../api/rest/errors';
import { addComment, removeTag, changeStatus } from '../../../../services/startrek';
import config from '../../../../services/config';
import { bodyValidator, getTestingDateComment, getTestingDateCommentSummonees } from './utils';
import { DraftStatus } from '../../../../db/tables/draft';

export const getDevicesTestingDateStatusHandler = asyncJsonResponse(
    async(req, res, log) => {
        const { skillId } = req.params;
        const { user, uid } = res.locals;

        log.addParams({ skillId, uid });

        log.info('Getting testing date status...');

        log.info('Checking user permissions to skill...');
        const skill = await findUserSkillWithId(skillId, user);

        try {
            const record = await getDevicesTestingRecord({ skillId });

            if (record) {
                log.info('Checking selected testing date status in startrek', { record });

                const status = await checkTestingDateSelectionStatus(skill);

                log.info('Got testing date meta', { record });

                return {
                    record,
                    status,
                };
            }
            log.info('Empty record', { record });

            return {
                status: TestingDateStatus.NOT_SPECIFIED,
            };
        } catch (e) {
            log.warn(e);

            throw e;
        }
    },
    {
        wrapResult: true,
        scope: 'getOnlineTestingDateHandler',
    },
);

export const createDevicesTestingDateHandler = asyncJsonResponse(
    async(req, res, log) => {
        const { skillId } = req.params;
        const { user, uid } = res.locals;
        const body = req.body;

        log.addParams({ skillId, uid, body });

        log.info('Validating body...');

        if (!bodyValidator(body)) {
            throw new RestApiError('Invalid body', 400);
        }

        log.info('Checking user permissions to skill...');
        const skill = await findUserSkillWithId(skillId, user);

        if (![DraftStatus.WaitingForTesting, DraftStatus.MeetingApproval].includes(skill.draft.status as DraftStatus)) {
            log.warn('Invalid draft status');

            throw new RestApiError('Invalid draft status', 403);
        }

        try {
            log.info('Creating devices testing record...');
            const record = await createOrUpdateDevicesTestingRecord({
                skillId,
                type: body.type,
                options: body.options,
            });

            const serializedRecord = serializeDevicesTestingRecord(record);

            try {
                log.warn('Trying to change ticket status...');
                await changeStatus(skill.draft.startrekTicket ?? '', 'meetingApproval');
            } catch (e) {
                log.warn(e);
            }

            if (skill.draft.status !== DraftStatus.MeetingApproval) {
                log.info('Updating draft status...');

                await skill.draft.update({
                    status: DraftStatus.MeetingApproval,
                });
            }

            log.info('Posting selected date to startrek...');

            await addComment(
                skill.draft.startrekTicket ?? '',
                getTestingDateComment(serializedRecord),
                await getTestingDateCommentSummonees(serializedRecord),
            );

            log.info('Removing tag');

            await removeTag(skill.draft.startrekTicket ?? '', config.startrek.smartHomeInvalidTestDateTag);

            log.info('Devices testing record created');

            return serializedRecord;
        } catch (e) {
            log.warn(e);

            throw e;
        }
    },
    {
        wrapResult: true,
        scope: 'createDevicesTestingDateHandler',
    },
);
