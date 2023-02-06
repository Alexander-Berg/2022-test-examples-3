import { CspPolicies } from '@yandex-int/nest-infra';

export const mdsTestingCspPreset: CspPolicies = {
    'script-src': ['s3.mdst.yandex.net'],
    'img-src': ['avatars.mdst.yandex.net', 's3.mdst.yandex.net'],
    'style-src': ['s3.mdst.yandex.net'],
    'font-src': ['s3.mdst.yandex.net'],
    'media-src': ['s3.mdst.yandex.net'],
    'frame-src': ['s3.mdst.yandex.net'],
    'child-src': ['s3.mdst.yandex.net'],
    'connect-src': ['s3.mdst.yandex.net'],
    'worker-src': ['s3.mdst.yandex.net'],
};
