import { Presets } from '@yandex-int/express-yandex-csp';

import defaultConfig from './defaults';
import mdsTestingCspPreset from './presets/mds-testing';
import passportCspPreset from './presets/passport';
import staticSelfCspPreset from './presets/static-self';

const presets: Presets = [...defaultConfig, mdsTestingCspPreset, passportCspPreset, staticSelfCspPreset];

export default presets;
