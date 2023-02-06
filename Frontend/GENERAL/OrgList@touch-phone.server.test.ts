import { assert } from 'chai';
import type { IPrivExternals } from '../../../../typings/legacy';
import type { ISerpDocument } from '../../../../typings';
import type { ISnippetContext } from '../../../../lib/Context/SnippetContext';
import type { IOneOrgPrivState } from '../../Companies.typings/IOneOrgPrivState';
import { AdapterOrgsList } from './OrgsList@touch-phone.server';
import type { IAdapterOrgsListSnippet } from './OrgsList.typings/index@touch-phone';

describe('AdapterOrgsList', () => {
    const document = {} as ISerpDocument;
    const privExternals = {
        Counter: () => null,
        pushAssets: () => null,
    } as unknown as IPrivExternals;
    let context: ISnippetContext;
    let snippet: IAdapterOrgsListSnippet;

    beforeEach(() => {
        context = {
            expFlags: {},
        } as ISnippetContext;
        snippet = {
            serp_info: {
                design: '',
            },
        } as IAdapterOrgsListSnippet;
    });

    describe('getItemsCount', function() {
        it('should return 7 by default', function() {
            const adapter = new AdapterOrgsList({ context, snippet, document, privExternals });

            assert.equal(adapter.getItemsCount(), 7);
        });

        it('should return 3 if viewType equal many_org_list3', function() {
            snippet.serp_info && (snippet.serp_info.design = 'many_org_list3');

            const adapter = new AdapterOrgsList({ context, snippet, document, privExternals });

            assert.equal(adapter.getItemsCount(), 3);
        });

        it('should return 5 if viewType equal many_org_list5', function() {
            snippet.serp_info && (snippet.serp_info.design = 'many_org_list5');

            const adapter = new AdapterOrgsList({ context, snippet, document, privExternals });

            assert.equal(adapter.getItemsCount(), 5);
        });

        it('should return 7 if viewType equal many_org_list7', function() {
            snippet.serp_info && (snippet.serp_info.design = 'many_org_list7');

            const adapter = new AdapterOrgsList({ context, snippet, document, privExternals });

            assert.equal(adapter.getItemsCount(), 7);
        });
    });

    describe('getCardOptionalLine', function() {
        it('should return reviewAspect', function() {
            const adapter = new AdapterOrgsList({ context, snippet, document, privExternals });
            // @ts-ignore
            const result = adapter.getCardOptionalLine({
                ugcAspects: {
                    aspect: 'вкусная еда',
                    positive_reviews_count: 4,
                    negative_reviews_count: 2,
                    reviews_count: 6,
                    is_trusted: true,
                },
            } as IOneOrgPrivState);

            assert.deepEqual(result, {
                reviewAspect: {
                    id: 'вкусная еда',
                    name: 'Вкусная еда',
                    negative: 2,
                    positive: 4,
                    reviews: 6,
                    untrusted: undefined,
                },
            });
        });

        it('should return first ten main aspects features', function() {
            const adapter = new AdapterOrgsList({ context, snippet, document, privExternals });
            // @ts-ignore
            const result = adapter.getCardOptionalLine({
                mainAspects: [
                    { aspect_name: 'Aspect #1' },
                    { aspect_name: 'Aspect #2' },
                    { aspect_name: 'Aspect #3' },
                    { aspect_name: 'Aspect #4' },
                    { aspect_name: 'Aspect #5' },
                    { aspect_name: 'Aspect #6' },
                    { aspect_name: 'Aspect #7' },
                    { aspect_name: 'Aspect #8' },
                    { aspect_name: 'Aspect #9' },
                    { aspect_name: 'Aspect #10' },
                    { aspect_name: 'Aspect #11' },
                ],
            } as IOneOrgPrivState);

            assert.deepEqual(result, {
                features: {
                    lines: 2,
                    items: [
                        'Aspect #1', 'Aspect #2', 'Aspect #3', 'Aspect #4', 'Aspect #5',
                        'Aspect #6', 'Aspect #7', 'Aspect #8', 'Aspect #9', 'Aspect #10',
                    ],
                },
            });
        });

        it('should return first ten core features', function() {
            const adapter = new AdapterOrgsList({ context, snippet, document, privExternals });
            // @ts-ignore
            const result = adapter.getCardOptionalLine({
                wzrdSubtype: 'lol',
                coreFeatures: ['1', '2', '3', '4', '5', '6', '7', '8', '9', '10', '11'],
                rawFeatures: {
                    '1': { name: 'Feature #1', value: 1 },
                    '2': { name: 'Feature #2', value: 1 },
                    '3': { name: 'Feature #3', value: 1 },
                    '4': { name: 'Feature #4', value: 1 },
                    '5': { name: 'Feature #5', value: 1 },
                    '6': { name: 'Feature #6', value: 1 },
                    '7': { name: 'Feature #7', value: 1 },
                    '8': { name: 'Feature #8', value: 1 },
                    '9': { name: 'Feature #9', value: 1 },
                    '10': { name: 'Feature #10', value: 1 },
                    '11': { name: 'Feature #11', value: 1 },
                },
            } as unknown as IOneOrgPrivState);

            assert.deepEqual(result, {
                features: {
                    lines: 2,
                    items: [
                        'Feature #1: 1', 'Feature #2: 1', 'Feature #3: 1', 'Feature #4: 1', 'Feature #5: 1',
                        'Feature #6: 1', 'Feature #7: 1', 'Feature #8: 1', 'Feature #9: 1', 'Feature #10: 1',
                    ],
                },
            });
        });

        it('should return ten merged main aspects and core features', function() {
            const adapter = new AdapterOrgsList({ context, snippet, document, privExternals });
            // @ts-ignore
            const result = adapter.getCardOptionalLine({
                mainAspects: [
                    { aspect_name: 'Aspect #1' },
                    { aspect_name: 'Aspect #2' },
                    { aspect_name: 'Aspect #3' },
                    { aspect_name: 'Aspect #4' },
                    { aspect_name: 'Aspect #5' },
                    { aspect_name: 'Aspect #6' },
                ],
                wzrdSubtype: 'lol',
                coreFeatures: ['1', '2', '3', '4', '5', '6', '7', '8', '9', '10', '11'],
                rawFeatures: {
                    '1': { name: 'Feature #1', value: 1 },
                    '2': { name: 'Feature #2', value: 1 },
                    '3': { name: 'Feature #3', value: 1 },
                    '4': { name: 'Feature #4', value: 1 },
                    '5': { name: 'Feature #5', value: 1 },
                    '6': { name: 'Feature #6', value: 1 },
                },
            } as IOneOrgPrivState);

            assert.deepEqual(result, {
                features: {
                    lines: 2,
                    items: [
                        'Aspect #1', 'Aspect #2', 'Aspect #3', 'Aspect #4', 'Aspect #5', 'Aspect #6',
                        'Feature #1: 1', 'Feature #2: 1', 'Feature #3: 1', 'Feature #4: 1',
                    ],
                },
            });
        });

        it('should return first ten main aspects and ignore core features', function() {
            const adapter = new AdapterOrgsList({ context, snippet, document, privExternals });
            // @ts-ignore
            const result = adapter.getCardOptionalLine({
                mainAspects: [
                    { aspect_name: 'Aspect #1' },
                    { aspect_name: 'Aspect #2' },
                    { aspect_name: 'Aspect #3' },
                    { aspect_name: 'Aspect #4' },
                    { aspect_name: 'Aspect #5' },
                    { aspect_name: 'Aspect #6' },
                    { aspect_name: 'Aspect #7' },
                    { aspect_name: 'Aspect #8' },
                    { aspect_name: 'Aspect #9' },
                    { aspect_name: 'Aspect #10' },
                    { aspect_name: 'Aspect #11' },
                ],
                wzrdSubtype: 'lol',
                coreFeatures: ['1', '2', '3', '4', '5', '6', '7', '8', '9', '10', '11'],
                rawFeatures: {
                    '1': { name: 'Feature #1', value: 1 },
                    '2': { name: 'Feature #2', value: 1 },
                    '3': { name: 'Feature #3', value: 1 },
                    '4': { name: 'Feature #4', value: 1 },
                    '5': { name: 'Feature #5', value: 1 },
                    '6': { name: 'Feature #6', value: 1 },
                },
            } as IOneOrgPrivState);

            assert.deepEqual(result, {
                features: {
                    lines: 2,
                    items: [
                        'Aspect #1', 'Aspect #2', 'Aspect #3', 'Aspect #4', 'Aspect #5',
                        'Aspect #6', 'Aspect #7', 'Aspect #8', 'Aspect #9', 'Aspect #10',
                    ],
                },
            });
        });

        it('should return empty object', function() {
            const adapter = new AdapterOrgsList({ context, snippet, document, privExternals });
            // @ts-ignore
            const result = adapter.getCardOptionalLine({} as IOneOrgPrivState);

            assert.deepEqual(result, {});
        });
    });

    describe('getMainAspects', function() {
        it('should return undefined if there is no mainAspects in state', function() {
            const adapter = new AdapterOrgsList({ context, snippet, document, privExternals });
            // @ts-ignore
            const result = adapter.getMainAspects({} as IOneOrgPrivState);

            assert.deepEqual(result, undefined);
        });
    });
});
