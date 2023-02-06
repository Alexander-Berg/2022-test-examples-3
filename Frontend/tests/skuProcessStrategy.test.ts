import type { ISearchResultSku } from '../sku';
import { skuProcessStrategy } from '../sku';
import { skuDataStub } from './skuStub';

describe('skuProcessStrategy', () => {
    describe('название магазина', () => {
        it('должно содержаться в offers если один оффер', () => {
            const result = skuProcessStrategy(skuDataStub as ISearchResultSku);
            expect(result.offers.defaultShopTitle).toEqual('OZON');
        });

        it('не должно содержаться если несколько офферов', () => {
            const result = skuProcessStrategy({ ...skuDataStub as ISearchResultSku, skuOffersCount: 2 });
            expect(result.offers.defaultShopTitle).toBeUndefined();
        });
    });
});
