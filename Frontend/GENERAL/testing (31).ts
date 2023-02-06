import { CspPresets } from '@yandex-int/nest-infra';

import { defaultCspPresets } from './defaults';
import { mdsTestingCspPreset } from './presets/mds-testing';
import { passportCspPreset } from './presets/passport';
import { staticSelfCspPreset } from './presets/static-self';

export const testingCspPresets: CspPresets = [
    ...defaultCspPresets,
    mdsTestingCspPreset,
    passportCspPreset,
    staticSelfCspPreset,
];
