/* eslint-disable @typescript-eslint/no-explicit-any */

import { ExpressHttpUatraitsOptions } from '@yandex-int/express-http-uatraits';

import { UatraitsConfigService } from '../uatraitsConfig.service';
import { UatraitsService } from '../uatraits.service';

// @ts-ignore
UatraitsService.prototype.getUatraits.disableMemoize();

const newUatraitsService = (req: any, options?: ExpressHttpUatraitsOptions) => {
    const config = new UatraitsConfigService(options);
    const context = { req } as any;

    return new UatraitsService(config, context);
};

describe('UatraitsService', () => {
    test('should just work', async () => {
        const req = {
            headers: {
                'user-agent': 'Mozilla/5.0 (Windows NT 6.1; WOW64; rv:40.0) Gecko/20100101 Firefox/40.1',
            },
        };
        const service = newUatraitsService(req);
        const uatraits = await service.getUatraits();

        expect(uatraits).toBeTruthy();
        expect(uatraits && uatraits.isBrowser).toBe(true);
    });

    test('should give correct result without user-agent header', async () => {
        const req = {
            headers: {},
        };

        const service = newUatraitsService(req);
        const uatraits = await service.getUatraits();

        expect(uatraits).toBe(null);
    });
});
