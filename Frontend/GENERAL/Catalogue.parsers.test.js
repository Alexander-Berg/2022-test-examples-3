import {
    prepareFilters,
    parseNodeData,
} from './Catalogue.parsers';

describe('Should transform data correctly', () => {
    describe('prepareFilters', () => {
        it('Check parse format with full data', () => {
            expect(prepareFilters({
                search: 'Hello world',
                member: ['binarycat', 'trshkv'],
                owner: ['ssav'],
                department: [10, 20, 30],
                states: ['develop', 'needinfo'],
                isSuspicious: [true],
                hasExternalMembers: [false],
                tags: [146],
            }, 123, [1, 2, 3])).toEqual({
                search: 'Hello world',
                member: ['binarycat', 'trshkv'],
                owner: ['ssav'],
                department: [10, 20, 30],
                state__in: 'develop,needinfo',
                is_suspicious: 'true',
                has_external_members: 'false',
                tags__id__in: '146',
                parents: '1,2,3',
                root: 123,
            });
        });

        it('Check parse format with empty data', () => {
            expect(() => prepareFilters()).toThrow(); // без аргументов сломается
            expect(prepareFilters({})).toEqual({
                department: undefined,
                member: undefined,
                owner: undefined,
                search: undefined,
                state__in: 'develop,supported,closed,needinfo',
                is_suspicious: null,
                has_external_members: null,
                parents: null,
                root: 0,
                tags__id__in: '',
            });
            expect(prepareFilters({}, 0)).toEqual({
                department: undefined,
                member: undefined,
                owner: undefined,
                search: undefined,
                state__in: 'develop,supported,closed,needinfo',
                is_suspicious: null,
                has_external_members: null,
                parents: null,
                root: 0,
                tags__id__in: '',
            });
            expect(prepareFilters({}, 0, [])).toEqual({
                department: undefined,
                member: undefined,
                owner: undefined,
                search: undefined,
                state__in: 'develop,supported,closed,needinfo',
                is_suspicious: null,
                has_external_members: null,
                parents: '',
                root: 0,
                tags__id__in: '',
            });
        });

        it('Should trim exhaustive data', () => {
            expect(prepareFilters({
                isSuspicious: [true, false],
                hasExternalMembers: [false, false],
            })).toMatchObject({
                is_suspicious: null,
                has_external_members: null,
            });
        });

        it('Should omit invalid params', () => {
            expect(prepareFilters({
                foo: 'bar',
            })).not.toMatchObject({
                foo: expect.anything(),
            });
        });
    });

    describe('parseNodeData', () => {
        it('Should select node info', () => {
            expect(parseNodeData({
                children_count: 0,
                created_at: '',
                has_external_members: false,
                has_forced_suspicious_reason: false,
                is_exportable: false,
                is_suspicious: false,
                modified_at: '',
                human_readonly_state: null,
                sandbox_move_date: {},
                unique_immediate_members_count: 0,
                unique_immediate_robots_count: 0,
                traffic_light: [],
                id: 0,
                kpi: {},
                level: 0,
                name: '',
                owner: {},
                parent: 0,
                slug: '',
                state: '',
                tags: [],
                type: '',
                matched: true,
            }))
                .toEqual({
                    childrenCount: 0,
                    createdAt: '',
                    hasExternalMembers: false,
                    hasForcedSuspiciousReason: false,
                    isExportable: false,
                    isSuspicious: false,
                    modifiedAt: '',
                    readonlyState: null,
                    sandboxMoveDate: {},
                    peopleCount: 0,
                    robotsCount: 0,
                    trafficLights: [],
                    id: 0,
                    kpi: {},
                    level: 0,
                    name: '',
                    owner: {},
                    parent: 0,
                    slug: '',
                    state: '',
                    tags: [],
                    type: '',
                    matched: true,
                    showAllFields: true,
                });
        });

        it('Should set `showAllFields` default value based on `matched`', () => {
            expect(parseNodeData({ matched: true }))
                .toMatchObject({ showAllFields: true });
        });

        it('Should prefer passed value for showAllFields', () => {
            expect(parseNodeData({ matched: true }, false))
                .toMatchObject({ showAllFields: false });
        });
    });
});
