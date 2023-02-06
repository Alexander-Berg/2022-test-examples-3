import { makeCreateAction } from '@/utils/actions';

import {
    ITestSendTask,
    IFanValidationError,
    IFanNotReadyError,
    IFanSendLimitError,
    IFanWrongDomainError,
    IFanWrongLoginError,
} from '@/../common/types';

import { FEATURE_NAME } from './constants';

export interface ITestSendStartPayload {
    recipients: string[];
    campaign_slug: string;
    user_template_variables: Record<string, string>;
}

const createAction = makeCreateAction(FEATURE_NAME);

export const testSend = createAction<string>('testSend');
export const testSendStart = createAction<ITestSendStartPayload>('testSendStart');
export const testSendAbort = createAction<void>('testSendAbort');
export const testSendSuccess = createAction<ITestSendTask>('testSendSuccess');
export const testSendFailure = createAction<
IFanValidationError | IFanNotReadyError | IFanWrongDomainError | IFanSendLimitError | IFanWrongLoginError | undefined
>('testSendFailure');

export {
    notifySuccess,
    notifyFailure,
    notifyGeneralFailure,
    showNotification,
} from '@/features/notifications/actions';
