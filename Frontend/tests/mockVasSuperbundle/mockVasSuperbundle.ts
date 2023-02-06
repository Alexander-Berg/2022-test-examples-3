import * as path from 'path';
import { mockNonWebpackRequire } from '../mockNonWebpackRequire/mockNonWebpackRequire';
import { GeneratedSuperbundleJSON } from '../../entries/server/build-time-val-loaders/loadVas.type';

export const MOCKED_STABLE_VERSION = 777;
const MOCKED_LOADER = 'adsdk.js';
export const MOCKED_VAS_BUNDLE_PATH = 'vas_bundle_path';

const MOCKED_SUPERBUNDLE: GeneratedSuperbundleJSON = {
    map: [[MOCKED_STABLE_VERSION, { version: MOCKED_STABLE_VERSION, content: MOCKED_LOADER }]],
    vpaidLoadersMap: [[MOCKED_STABLE_VERSION, { version: MOCKED_STABLE_VERSION, content: MOCKED_LOADER }]],
    stable: MOCKED_STABLE_VERSION,
    config: {
        loader: 1648456670321,
        stable: MOCKED_STABLE_VERSION,
        code: [],
    },
};

export const mockVasSuperbundle = () => {
    mockNonWebpackRequire(path.join(process.cwd(), MOCKED_VAS_BUNDLE_PATH), MOCKED_SUPERBUNDLE);
};
