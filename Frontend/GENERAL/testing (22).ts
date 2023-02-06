import { Presets } from '@yandex-int/express-yandex-csp';

import defaultConfig from 'src/configs/csp/defaults';
import mdsTestingCspPreset from 'src/configs/csp/presets/mds-testing';
import passportCspPreset from 'src/configs/csp/presets/passport';
import staticSelfCspPreset from 'src/configs/csp/presets/static-self';

const presets: Presets = [...defaultConfig, mdsTestingCspPreset, passportCspPreset, staticSelfCspPreset];

export default presets;
