import { getGeneralBaseHash } from './workCopyUtils';
import { WorkCopy, CheckoutConfig } from './types';

describe('workCopyUtils', () => {
    describe('getGeneralBaseHash', () => {
        it('should return general base hash for common config', () => {
            const checkoutConfig: CheckoutConfig = {
                base: [
                    { commit: 'base1', branch: 'br1' } as WorkCopy,
                    { commit: 'base2', branch: 'br2' } as WorkCopy
                ],
                head: { commit: 'head', branch: 'br3' } as WorkCopy
            };

            expect(getGeneralBaseHash(checkoutConfig)).toEqual('base1-base2-head');
        });

        it('should return empty base hash for config with empty commits', () => {
            const checkoutConfig: CheckoutConfig = {};

            expect(getGeneralBaseHash(checkoutConfig)).toEqual('');
        });
    });
});
