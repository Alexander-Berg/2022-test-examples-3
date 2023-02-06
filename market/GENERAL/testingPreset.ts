import {CSPPresetsArray} from 'csp-header';

import defaultsPreset from './defaultsPreset';
import {passportDirectives, staticSelfDirectives} from '../directives';

const presets: CSPPresetsArray = [...defaultsPreset, passportDirectives, staticSelfDirectives];

export default presets;
