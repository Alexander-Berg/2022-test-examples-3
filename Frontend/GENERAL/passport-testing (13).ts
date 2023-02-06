import { CSP, CspPolicies } from '@yandex-int/nest-infra';

export const passportTestingCspPreset: CspPolicies = {
    'script-src': [`pass-test.yandex.${CSP.TLD}`, 'social.yandex.ru'],
};
