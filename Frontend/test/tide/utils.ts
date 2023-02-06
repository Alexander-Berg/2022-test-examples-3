import _ from 'lodash';
import { Tide, TideConfig } from '../../src';

export function mkTide(config?: Partial<TideConfig>): Tide {
    const tide = new Tide(
        _.defaultsDeep({}, config ?? {}, {
            silent: true,
            plugins: {
                'tide-usage-stats': { enabled: false },
            },
            cacheDir: '/fixtures/cache',
        }),
    );

    return tide;
}
