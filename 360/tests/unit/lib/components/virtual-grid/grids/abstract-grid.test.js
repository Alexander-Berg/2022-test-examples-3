import AbstractGrid, { DELTA_COUNTED_BY } from '../../../../../../lib/components/virtual-grid/grids/abstract-grid';

describe('AbstractGrid', () => {
    describe('_calcVisibleItemsDelta', () => {
        let grid;
        beforeEach(() => {
            grid = new AbstractGrid({
                items: []
            });
        });

        it('should calc delta by cluster', () => {
            expect(grid._calcVisibleItemsDelta({
                clusters: [{
                    id: 'cluster1',
                    absoluteTop: 100
                }],
                items: []
            }, {}, 'cluster', {
                id: 'cluster1',
                absoluteTop: 120
            })).toEqual({
                countedBy: DELTA_COUNTED_BY.CLUSTER,
                delta: 20
            });
        });
        it('should calc delta by resource', () => {
            expect(grid._calcVisibleItemsDelta({
                clusters: [],
                items: [{
                    id: 'resource1',
                    absoluteTop: 500
                }]
            }, {}, 'item', {
                id: 'resource1',
                absoluteTop: 300
            })).toEqual({
                countedBy: DELTA_COUNTED_BY.RESOURCE,
                delta: -200
            });
        });
        it('should calc delta by index path', () => {
            expect(grid._calcVisibleItemsDelta({
                clusters: [],
                items: [{
                    clusterIndex: 5,
                    resourceIndex: 100,
                    absoluteTop: 1000
                }]
            }, {}, 'item', {
                clusterIndex: 5,
                resourceIndex: 100,
                absoluteTop: 300
            })).toEqual({
                countedBy: DELTA_COUNTED_BY.INDEX_PATH,
                delta: -700
            });
        });
        it('should calc delta by index path even if resource has id', () => {
            expect(grid._calcVisibleItemsDelta({
                clusters: [],
                items: [{
                    clusterIndex: 5,
                    resourceIndex: 100,
                    absoluteTop: 1000
                }]
            }, {}, 'item', {
                clusterIndex: 5,
                resourceIndex: 100,
                id: 'resource1',
                absoluteTop: 300
            })).toEqual({
                countedBy: DELTA_COUNTED_BY.INDEX_PATH,
                delta: -700
            });
        });

        it('should not calc delta by cluster if already counted by resource', () => {
            const prevVisibleItemsDelta = {
                countedByResource: true,
                delta: 0
            };
            expect(grid._calcVisibleItemsDelta({
                clusters: [{
                    id: 'cluster1',
                    absoluteTop: 100
                }],
                items: []
            }, prevVisibleItemsDelta, 'cluster', {
                id: 'resource1',
                absoluteTop: 300
            })).toBe(prevVisibleItemsDelta);
        });
        it('should not calc delta by index path if already counted by resource', () => {
            const prevVisibleItemsDelta = {
                countedBy: DELTA_COUNTED_BY.RESOURCE,
                delta: 1
            };
            expect(grid._calcVisibleItemsDelta({
                clusters: [],
                items: [{
                    clusterIndex: 1,
                    resourceIndex: 5,
                    absoluteTop: 300
                }]
            }, prevVisibleItemsDelta, 'item', {
                clusterIndex: 1,
                resourceIndex: 5,
                absoluteTop: 400
            })).toBe(prevVisibleItemsDelta);
        });
        it('should not calc delta by index path if already counted by cluster', () => {
            const prevVisibleItemsDelta = {
                countedBy: DELTA_COUNTED_BY.CLUSTER,
                delta: 2
            };
            expect(grid._calcVisibleItemsDelta({
                clusters: [],
                items: [{
                    clusterIndex: 1,
                    resourceIndex: 5,
                    absoluteTop: 300
                }]
            }, prevVisibleItemsDelta, 'item', {
                clusterIndex: 1,
                resourceIndex: 5,
                absoluteTop: 400
            })).toBe(prevVisibleItemsDelta);
        });

        it('should calc delta by cluster if already counted by index path', () => {
            expect(grid._calcVisibleItemsDelta({
                clusters: [{
                    id: 'cluster1',
                    absoluteTop: 100
                }],
                items: []
            }, {
                countedBy: DELTA_COUNTED_BY.INDEX_PATH,
                delta: 2
            }, 'cluster', {
                id: 'cluster1',
                absoluteTop: 200
            })).toEqual({
                countedBy: DELTA_COUNTED_BY.CLUSTER,
                delta: 100
            });
        });
        it('should calc delta by resource if already counted by cluster', () => {
            expect(grid._calcVisibleItemsDelta({
                clusters: [],
                items: [{
                    id: 'resource1',
                    absoluteTop: 158
                }]
            }, {
                countedBy: DELTA_COUNTED_BY.CLUSTER,
                delta: 3
            }, 'item', {
                id: 'resource1',
                absoluteTop: 200
            })).toEqual({
                countedBy: DELTA_COUNTED_BY.RESOURCE,
                delta: 42
            });
        });
        it('should calc delta by resource if already counted by index path', () => {
            expect(grid._calcVisibleItemsDelta({
                clusters: [],
                items: [{
                    id: 'resource1',
                    absoluteTop: 159
                }]
            }, {
                countedBy: DELTA_COUNTED_BY.INDEX_PATH,
                delta: 3
            }, 'item', {
                id: 'resource1',
                absoluteTop: 200
            })).toEqual({
                countedBy: DELTA_COUNTED_BY.RESOURCE,
                delta: 41
            });
        });

        it('should not calc delta by cluster if already counted by another cluster', () => {
            const prevVisibleItemsDelta = {
                countedBy: DELTA_COUNTED_BY.CLUSTER,
                delta: 3453245
            };
            expect(grid._calcVisibleItemsDelta({
                clusters: [{
                    id: 'cluster2',
                    absoluteTop: 100
                }],
                items: []
            }, prevVisibleItemsDelta, 'cluster', {
                id: 'cluster2',
                absoluteTop: 200
            })).toBe(prevVisibleItemsDelta);
        });
        it('should not calc delta by resource if already counted by another resource', () => {
            const prevVisibleItemsDelta = {
                countedBy: DELTA_COUNTED_BY.RESOURCE,
                delta: 9
            };
            expect(grid._calcVisibleItemsDelta({
                clusters: [],
                items: [{
                    id: 'resource2',
                    absoluteTop: 158
                }]
            }, prevVisibleItemsDelta, 'item', {
                id: 'resource2',
                absoluteTop: 200
            })).toBe(prevVisibleItemsDelta);
        });
        it('should not calc delta by index path if already counted by another index path', () => {
            const prevVisibleItemsDelta = {
                countedBy: DELTA_COUNTED_BY.INDEX_PATH,
                delta: 990
            };
            expect(grid._calcVisibleItemsDelta({
                clusters: [],
                items: [{
                    clusterIndex: 5,
                    resourceIndex: 6,
                    absoluteTop: 159
                }]
            }, prevVisibleItemsDelta, 'item', {
                clusterIndex: 5,
                resourceIndex: 6,
                absoluteTop: 200
            })).toBe(prevVisibleItemsDelta);
        });
    });
});
