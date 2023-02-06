import { CspPresets } from '@yandex-int/nest-infra';

import defaultConfig from './defaults';
import mdsTestingCspPreset from './presets/mds-testing';
import passportCspPreset from './presets/passport';
import staticSelfCspPreset from './presets/static-self';

const presets: CspPresets = [...defaultConfig, mdsTestingCspPreset, passportCspPreset, staticSelfCspPreset];

export default presets;
