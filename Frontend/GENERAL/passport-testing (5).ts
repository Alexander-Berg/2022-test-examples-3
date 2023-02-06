import { CSP, CspPolicies } from '@yandex-int/nest-infra';

const policies: CspPolicies = {
    'script-src': [`pass-test.yandex.${CSP.TLD}`, 'social.yandex.ru'],
};

export default policies;
