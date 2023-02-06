import { put, call, select } from 'typed-redux-saga';

import { IFanValidationError, IFanSendLimitError, IFanWrongLoginError } from '@/../common/types';
import { NotificationLevel, NotificationType } from '@/features/notifications/enums';
import { testSendDone } from '@/locales/i18n/mail-liza/fan-campaign-form';
import {
    sendLimitErrorAction,
    sendLimitErrorText,
    testSendCantSend,
} from '@/locales/i18n/mail-liza/fan-errors';
import { help } from '@/config/urls';
import { sendMetrika } from '@/metrika';

import {
    testSendStart,
    testSendSuccess,
    testSendFailure,
    notifySuccess,
    notifyFailure,
    showNotification,
    notifyGeneralFailure,
} from '../actions';
import { getActiveProjectSlugSafe } from '../selectors';
import { requestTestSendTask, isModelResolved } from '../transport';
import { getErrorText } from '../utils/getErrorText';

export const SAGA_NAME = 'testSend';

const saga = function* (action: ReturnType<typeof testSendStart>) {
    const activeProjectSlug = yield* select(getActiveProjectSlugSafe);
    const { campaign_slug, recipients, user_template_variables } = action.payload;

    const hasAtLeastOneVariable = Object.values(user_template_variables).filter(v => v).length > 0;

    if (hasAtLeastOneVariable) {
        yield* call(sendMetrika, 'Test mail popup', 'enter variable');
    }

    const [testSendTask] = yield* call(
        requestTestSendTask,
        activeProjectSlug,
        campaign_slug,
        recipients,
        user_template_variables,
    );

    if (isModelResolved(testSendTask)) {
        yield* put(testSendSuccess(testSendTask.data));
        yield* put(notifySuccess(testSendDone));
    } else {
        const error = testSendTask.error as unknown as IFanValidationError |
                IFanSendLimitError | IFanWrongLoginError;

        yield* put(testSendFailure(error));

        switch (error?.error) {
            case 'limit_reached': {
                yield* put(showNotification({
                    text: sendLimitErrorText,
                    autoclosable: true,
                    type: NotificationType.action,
                    level: NotificationLevel.error,
                    actions: [
                        {
                            text: sendLimitErrorAction,
                            url: `${help}/bulk-email-rules.html#bulk-email-rules__limit`,
                        },
                    ],
                }));
                break;
            }

            case 'wrong_login': {
                yield* put(notifyFailure(getErrorText(error.detail)));
                break;
            }

            default: {
                yield* put(notifyFailure(testSendCantSend));
            }
        }
    }
};

export { saga };

export function* errorHandler() {
    yield* put(notifyGeneralFailure());
    yield* put(testSendFailure());
}
