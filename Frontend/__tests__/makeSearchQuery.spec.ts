import { BackendSearchQuery } from '../../store/search/searchDomain';
import { makeCustomArgs, makeArchivedFilter, makeDeletedFilter, makeCircuits, makeVersionsPolicy, makeVerdicts, makeSearchQuery } from '../query/makeSearchQuery';

describe('utils/makeCustomArgs', () => {
    it('makes right custom args from config', () => {
        expect(makeCustomArgs('direct', false)).toEqual({ custom: { custom_types: ['licenses', 'domain_warnings', 'threats', 'text_highlight'] } });
    });

    it('removes text_highlight from custom', () => {
        expect(makeCustomArgs('direct', true)).toEqual({ custom: { custom_types: ['licenses', 'domain_warnings', 'threats'] } });
    });
});

describe('utils/makeArchivedFilter', () => {
    it('makes empty history if query with archived', () => {
        expect(makeArchivedFilter('direct', { type: [], with_archive: true })).toEqual({});
    });

    it('makes archived filter', () => {
        expect(makeArchivedFilter('direct', { type: [], with_archive: false })).toEqual({ archived: false });
    });
});

describe('utils/makeDeletedFilter', () => {
    it('makes deleted filter', () => {
        expect(makeDeletedFilter('direct', { type: [], deleted: true })).toEqual({ deleted: true });
    });

    it('checks object_id', () => {
        expect(makeDeletedFilter('direct', { type: [], object_id: '1' })).toEqual({});
    });
});

describe('utils/makeCircuits', () => {
    it('add circuits if search page', () => {
        expect(makeCircuits(true)).toEqual({ circuits: ['VTOTAL', 'TOTAL', 'moderation', 'moderation_update_flags', 'admin_interface'] });
    });
});

describe('utils/makeVersionsPolicy', () => {
    it('adds right versions policy', () => {
        expect(makeVersionsPolicy(false)).toEqual({ versions_policy: 'LAST' });
    });
});

describe('utils/makesVerdictFilter', () => {
    it('adds negative verdict filter', () => {
        expect(makeVerdicts('no')).toEqual({ verdict: 'No' });
    });

    it('adds none verdict filter', () => {
        expect(makeVerdicts('none')).toEqual({ verdict: null });
    });
});

describe('utils/makeSearchQuery', () => {
    const queryExample = {
        type: ['banner', 'text_sm'],
        exactly_last: true,
        with_archive: true,
        without_highlight: false,
        campaign_id: [42509534],

    } as BackendSearchQuery;

    it('returns query for search page', () => {
        expect(makeSearchQuery(queryExample, 'direct', true)).toEqual(
            {
                type: [
                    'banner',
                    'text_sm',
                ],
                campaign_id: [
                    42509534,
                ],
                deleted: false,
                versions_policy: 'EXACTLY_LAST_AFTER_FILTER',
                custom: {
                    custom_types: [
                        'licenses',
                        'domain_warnings',
                        'threats',
                        'text_highlight',
                    ],
                },
                circuits: [
                    'VTOTAL',
                    'TOTAL',
                    'moderation',
                    'moderation_update_flags',
                    'admin_interface',
                ],
            },
        );
    });

    it('returns query for other pages', () => {
        expect(makeSearchQuery(queryExample, 'direct', false)).toEqual(
            {
                type: [
                    'banner',
                    'text_sm',
                ],
                campaign_id: [
                    42509534,
                ],
                deleted: false,
                versions_policy: 'EXACTLY_LAST_AFTER_FILTER',
                custom: {
                    custom_types: [
                        'licenses',
                        'domain_warnings',
                        'threats',
                        'text_highlight',
                    ],
                },
                verdict: undefined,
            },
        );
    });
});
