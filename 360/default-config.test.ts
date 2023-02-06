/// <reference types="jest" />

import { defaultConfig } from './default-config';

describe('default config', () => {
    it('no env', () => {
        const conf = defaultConfig();

        expect(conf).toHaveProperty('url');
        expect(conf).toMatchSnapshot();
    });

    it('testing', () => {
        const conf = defaultConfig({ ENVIRONMENT_TYPE: 'testing' });

        expect(conf).toHaveProperty('url', expect.stringMatching('api-internal-test'));
    });
});
