import { CSPPresetsArray } from 'csp-header';

import defaultConfig from './defaults';
import mdsTestingCspPreset from './presets/mds-testing';
import passportCspPreset from './presets/passport';
import statementsTestingCspPreset from './presets/statements-testing';

const presets: CSPPresetsArray = [
    ...defaultConfig,
    mdsTestingCspPreset,
    passportCspPreset,
    statementsTestingCspPreset,
];

export default presets;
