import { PlaywrightTestConfig } from '@playwright/test';

import config from './config';

const playwrightConfig: PlaywrightTestConfig = {
    use: {
        baseURL: config.origin,
        trace: 'retain-on-failure',
    },
    webServer: {
        command: `NODE_ENV=development yarn webpack-dev-server --port=${config.port}`,
        port: config.port,
        cwd: '../',
        reuseExistingServer: !process.env.TEST_STANDALONE,
    },
};

export default playwrightConfig;
