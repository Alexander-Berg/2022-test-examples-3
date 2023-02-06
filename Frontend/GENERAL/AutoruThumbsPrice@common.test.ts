import { assert } from 'chai';
import type { IExpFlag, IPrivExternals, Shared } from '@typings';
import type { ISnippetContext } from '@lib/Context';
import { AdapterAutoruThumbsPrice } from './AutoruThumbsPrice@common.server';
import type { AutoruThumbsPrice as A } from './AutoruThumbsPrice.typings';

function createAutoruThumbsPrice(
    snippet: A.ISnippet,
): AdapterAutoruThumbsPrice {
    const context = {
        expFlags: {
            organicable_auto: 0,
        } as unknown as IExpFlag,

        reportData: {
            pushBundle() {},
        } as ISnippetContext['reportData'],
    } as ISnippetContext;

    return new AdapterAutoruThumbsPrice({
        privExternals: {} as unknown as IPrivExternals,
        context,
        snippet,
    });
}

describe('AdapterAutoruThumbsPrice@common', () => {
    describe('transform', () => {
        it('параметр url соответствует ожидаемому формату', () => {
            const urlAutoRu = 'https://auto.ru';

            const adapterAutoru = createAutoruThumbsPrice({
                title: {
                    text: 'title',
                    url: urlAutoRu,
                },
                path: {
                    items: [{ text: 'path' }],
                },
                text: 'text',
                counter: {},
            });

            const organic = adapterAutoru.transform();

            const expectedUrlObject: Shared.IUrlParamObject = { url: urlAutoRu };

            assert.deepEqual(organic.url, expectedUrlObject);
        });
    });
});
