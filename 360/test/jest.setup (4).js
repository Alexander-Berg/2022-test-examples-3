/* global jest */

[
    '@yandex-int/rum-counter/dist/bundle/implementation',
    '@yandex-int/rum-counter/dist/bundle/onload',
    '@yandex-int/rum-counter/dist/bundle/send',
    '@yandex-int/error-counter/dist/logError',
].forEach(path => {
    jest.mock(path, () => undefined);
});

[
    '@/locales/i18n/mail-liza/fan-campaign-form',
    '@/locales/i18n/mail-liza/fan-campaigns',
    '@/locales/i18n/mail-liza/fan-common',
    '@/locales/i18n/mail-liza/fan-common-templates',
    '@/locales/i18n/mail-liza/fan-project-templates',
    '@/locales/i18n/mail-liza/fan-errors',
    '@/locales/i18n/mail-liza/fan-grapes',
    '@/locales/i18n/mail-liza/fan-manage-projects',
    '@/locales/i18n/mail-liza/fan-notification',
    '@/locales/i18n/mail-liza/fan-onboarding',
    '@/locales/i18n/mail-liza/fan-organization',
    '@/locales/i18n/mail-liza/fan-promos',
].forEach(path => jest.mock(path, () => ({})));

jest.mock('@/config');
jest.mock('@/utils/rum');
jest.mock('@/utils/transport');
