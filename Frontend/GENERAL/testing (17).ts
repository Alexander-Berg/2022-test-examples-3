import { Presets } from 'express-yandex-csp';

import defaultConfig from './defaults';
import mdsTestingCspPreset from './presets/mds-testing';
import passportCspPreset from './presets/passport';
import staticSelfCspPreset from './presets/static-self';
import qrCodeTestingCspPreset from './presets/qr-code-image-testing';

const presets: Presets = [
    ...defaultConfig,
    mdsTestingCspPreset,
    passportCspPreset,
    staticSelfCspPreset,
    qrCodeTestingCspPreset,
    {
        'img-src': ['events-test.paysys.yandex.ru'],
    },
];

export default presets;
