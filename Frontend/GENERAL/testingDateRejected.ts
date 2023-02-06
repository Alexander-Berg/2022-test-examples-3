/* eslint-disable */
import { asyncJsonResponse } from '../../../api/rest/utils';
import { getSmartHomeDraftOnModerationByTicket } from '../moderation/utils';
import { sendInappropriateTestingDateNotification } from '../../../services/mail';

export const notifyTestingDateRejected = asyncJsonResponse(
    async(req, res, log) => {
        const { key } = req.body;

        try {
            log.info('Finding smart home skill by ticket...');
            const skill = await getSmartHomeDraftOnModerationByTicket(key);

            const user = (await skill.getUser())!;

            log.info('Sending email...');
            await sendInappropriateTestingDateNotification(user, skill.id, skill.draft.name);

            log.info('Email sent');
        } catch (e) {
            log.warn(e);

            throw e;
        }
    },
    {
        scope: 'notifyTestingDateRejected',
    },
);
