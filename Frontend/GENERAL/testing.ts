import { Presets } from 'express-yandex-csp';

import defaultConfig from './defaults';
import mdsTestingCspPreset from './presets/mds-testing';
import passportCspPreset from './presets/passport';<% if (shouldAddSentry) { %>
import sentryTestingCspPreset from './presets/sentry-testing';<% } %>
import staticSelfCspPreset from './presets/static-self';

const presets: Presets = [
    ...defaultConfig,
    mdsTestingCspPreset,
    passportCspPreset,<% if (shouldAddSentry) { %>
    sentryTestingCspPreset,<% } %>
    staticSelfCspPreset
];

export default presets;
