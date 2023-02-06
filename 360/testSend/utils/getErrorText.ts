import {
    testSendInvalidAddress,
    testSendMaximumAddresses,
    testSendNoEmails,
    fieldFromLoginNotBelongsError,
} from '@/locales/i18n/mail-liza/fan-errors';
import { somethingWentWrong } from '@/locales/i18n/mail-liza/fan-common';

const ERRORS: Record<string, string> = {
    invalid_email: testSendInvalidAddress,
    too_long: testSendMaximumAddresses,
    empty: testSendNoEmails,
    not_belongs: fieldFromLoginNotBelongsError,
};

export function getErrorText(errorCode: string) {
    return ERRORS[errorCode] || somethingWentWrong;
}
