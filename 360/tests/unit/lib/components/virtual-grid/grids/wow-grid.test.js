import WowGrid from '../../../../../../lib/components/virtual-grid/grids/wow-grid';

describe('WowGrid', () => {
    const horizontalItem = { width: 400, height: 300 };
    const verticalItem = { width: 300, height: 400 };
    const squareItem = { width: 300, height: 300 };
    const panoramaItem = { width: 700, height: 300 };

    describe('_getRowLayoutFallback', () => {
        const getGrid = (columns = 10) => new WowGrid({
            columns,
            items: []
        });

        it('build default grid for 1 horizontal item', () => {
            expect(getGrid()._getRowLayoutFallback(1, [horizontalItem])).toEqual([{
                height: 3,
                layout: [{
                    layoutX: 0,
                    layoutY: 0,
                    layoutWidth: 3,
                    layoutHeight: 3
                }],
                indexFrom: 0
            }]);
        });
        it('build default grid for 1 vertical item', () => {
            expect(getGrid()._getRowLayoutFallback(1, [verticalItem])).toEqual([{
                height: 3,
                layout: [{
                    layoutX: 0,
                    layoutY: 0,
                    layoutWidth: 2,
                    layoutHeight: 3
                }],
                indexFrom: 0
            }]);
        });
        it('build default grid for 1 square item', () => {
            expect(getGrid()._getRowLayoutFallback(1, [squareItem])).toEqual([{
                height: 3,
                layout: [{
                    layoutX: 0,
                    layoutY: 0,
                    layoutWidth: 2,
                    layoutHeight: 3
                }],
                indexFrom: 0
            }]);
        });
        it('build default grid for 1 panorama item', () => {
            expect(getGrid()._getRowLayoutFallback(1, [panoramaItem])).toEqual([{
                height: 3,
                layout: [{
                    layoutX: 0,
                    layoutY: 0,
                    layoutWidth: 4,
                    layoutHeight: 3
                }],
                indexFrom: 0
            }]);
        });
        it('build default grid for few items in one row', () => {
            expect(getGrid()._getRowLayoutFallback(3, [panoramaItem, verticalItem, horizontalItem])).toEqual([{
                height: 3,
                layout: [{
                    layoutX: 0,
                    layoutY: 0,
                    layoutWidth: 4,
                    layoutHeight: 3
                }, {
                    layoutX: 4,
                    layoutY: 0,
                    layoutWidth: 2,
                    layoutHeight: 3
                }, {
                    layoutX: 6,
                    layoutY: 0,
                    layoutWidth: 3,
                    layoutHeight: 3
                }],
                indexFrom: 0
            }]);
        });
        it('build default grid for few rows w/o stretch', () => {
            expect(getGrid()._getRowLayoutFallback(4, [panoramaItem, horizontalItem, horizontalItem, verticalItem])).toEqual([{
                height: 3,
                layout: [{
                    layoutX: 0,
                    layoutY: 0,
                    layoutWidth: 4,
                    layoutHeight: 3
                }, {
                    layoutX: 4,
                    layoutY: 0,
                    layoutWidth: 3,
                    layoutHeight: 3
                }, {
                    layoutX: 7,
                    layoutY: 0,
                    layoutWidth: 3,
                    layoutHeight: 3
                }],
                indexFrom: 0
            }, {
                height: 3,
                layout: [{
                    layoutX: 0,
                    layoutY: 0,
                    layoutWidth: 2,
                    layoutHeight: 3
                }],
                indexFrom: 3
            }]);
        });
        it('build default grid for few rows with stretch', () => {
            expect(getGrid()._getRowLayoutFallback(4, [panoramaItem, verticalItem, horizontalItem, horizontalItem])).toEqual([{
                height: 3,
                layout: [{
                    layoutX: 0,
                    layoutY: 0,
                    layoutWidth: 5,
                    layoutHeight: 3
                }, {
                    layoutX: 5,
                    layoutY: 0,
                    layoutWidth: 2,
                    layoutHeight: 3
                }, {
                    layoutX: 7,
                    layoutY: 0,
                    layoutWidth: 3,
                    layoutHeight: 3
                }],
                indexFrom: 0
            }, {
                height: 3,
                layout: [{
                    layoutX: 0,
                    layoutY: 0,
                    layoutWidth: 3,
                    layoutHeight: 3
                }],
                indexFrom: 3
            }]);
        });
        it('build default grid for few rows with more stretch', () => {
            expect(getGrid()._getRowLayoutFallback(4, [verticalItem, horizontalItem, verticalItem, panoramaItem])).toEqual([{
                height: 3,
                layout: [{
                    layoutX: 0,
                    layoutY: 0,
                    layoutWidth: 3,
                    layoutHeight: 3
                }, {
                    layoutX: 3,
                    layoutY: 0,
                    layoutWidth: 4,
                    layoutHeight: 3
                }, {
                    layoutX: 7,
                    layoutY: 0,
                    layoutWidth: 3,
                    layoutHeight: 3
                }],
                indexFrom: 0
            }, {
                height: 3,
                layout: [{
                    layoutX: 0,
                    layoutY: 0,
                    layoutWidth: 4,
                    layoutHeight: 3
                }],
                indexFrom: 3
            }]);
        });
        it('build default grid for few rows with more than 1 column stretch', () => {
            expect(getGrid()._getRowLayoutFallback(3, [panoramaItem, horizontalItem, panoramaItem])).toEqual([{
                height: 3,
                layout: [{
                    layoutX: 0,
                    layoutY: 0,
                    // панорама растянулась на +2, так как растяжения +1 не хватало
                    layoutWidth: 6,
                    layoutHeight: 3
                }, {
                    layoutX: 6,
                    layoutY: 0,
                    layoutWidth: 4,
                    layoutHeight: 3
                }],
                indexFrom: 0
            }, {
                height: 3,
                layout: [{
                    layoutX: 0,
                    layoutY: 0,
                    layoutWidth: 4,
                    layoutHeight: 3
                }],
                indexFrom: 2
            }]);
        });

        it('build default grid for touch', () => {
            expect(getGrid(6)._getRowLayoutFallback(5, [verticalItem, verticalItem, horizontalItem, horizontalItem, panoramaItem])).toEqual([{
                height: 3,
                layout: [{
                    layoutX: 0,
                    layoutY: 0,
                    layoutWidth: 3,
                    layoutHeight: 3
                }, {
                    layoutX: 3,
                    layoutY: 0,
                    layoutWidth: 3,
                    layoutHeight: 3
                }],
                indexFrom: 0
            }, {
                height: 3,
                layout: [{
                    layoutX: 0,
                    layoutY: 0,
                    layoutWidth: 3,
                    layoutHeight: 3
                }, {
                    layoutX: 3,
                    layoutY: 0,
                    layoutWidth: 3,
                    layoutHeight: 3
                }],
                indexFrom: 2
            }, {
                height: 3,
                layout: [{
                    layoutX: 0,
                    layoutY: 0,
                    layoutWidth: 4,
                    layoutHeight: 3
                }],
                indexFrom: 4
            }]);
        });

        it('build default grid for touch (2 panoramas / panorama + horizontal / 2 horizontal)', () => {
            const expectedLayout = [{
                height: 3,
                layout: [{
                    layoutX: 0,
                    layoutY: 0,
                    layoutWidth: 3,
                    layoutHeight: 3
                }, {
                    layoutX: 3,
                    layoutY: 0,
                    layoutWidth: 3,
                    layoutHeight: 3
                }],
                indexFrom: 0
            }];
            expect(getGrid(6)._getRowLayoutFallback(2, [panoramaItem, panoramaItem])).toEqual(expectedLayout);
            expect(getGrid(6)._getRowLayoutFallback(2, [panoramaItem, horizontalItem])).toEqual(expectedLayout);
            expect(getGrid(6)._getRowLayoutFallback(2, [horizontalItem, panoramaItem])).toEqual(expectedLayout);
            expect(getGrid(6)._getRowLayoutFallback(2, [horizontalItem, horizontalItem])).toEqual(expectedLayout);
        });
    });

    describe('_getWithoutSizeFallback', () => {
        it('should split size into pieces between 5 and 9 for maxLength = 9', () => {
            const grid = new WowGrid({
                columns: 10,
                items: [],
                layouts: {
                    defaultLayouts: {
                        maxLength: 9
                    }
                }
            });
            grid._getDefaultLayout = (size) => size;

            const runTest = (size, expectedSplit) => {
                expect(grid._getWithoutSizeFallback(size)).toEqual(expectedSplit);
            };

            runTest(5, [5]);
            runTest(6, [6]);
            runTest(7, [7]);
            runTest(8, [8]);
            runTest(9, [9]);
            runTest(10, [5, 5]);
            runTest(11, [6, 5]);
            runTest(12, [7, 5]);
            runTest(13, [8, 5]);
            runTest(14, [9, 5]);
            runTest(15, [9, 6]);
            runTest(16, [9, 7]);
            runTest(17, [9, 8]);
            runTest(18, [9, 9]);
            runTest(19, [9, 5, 5]);
            runTest(20, [9, 6, 5]);
            runTest(21, [9, 7, 5]);
            runTest(22, [9, 8, 5]);
            runTest(23, [9, 9, 5]);
            runTest(24, [9, 9, 6]);
            runTest(25, [9, 9, 7]);
            runTest(26, [9, 9, 8]);
            runTest(27, [9, 9, 9]);
            runTest(28, [9, 9, 5, 5]);

            runTest(99, [9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9]);
            runTest(100, [9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 5, 5]);
            runTest(101, [9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 6, 5]);
        });

        it('should split size into pieces between 2 and 3 for maxLength = 3', () => {
            const grid = new WowGrid({
                columns: 6,
                items: [],
                layouts: {
                    defaultLayouts: {
                        maxLength: 3
                    }
                }
            });
            grid._getDefaultLayout = (size) => size;

            const runTest = (size, expectedSplit) => {
                expect(grid._getWithoutSizeFallback(size)).toEqual(expectedSplit);
            };

            runTest(2, [2]);
            runTest(3, [3]);
            runTest(4, [2, 2]);
            runTest(5, [3, 2]);
            runTest(6, [3, 3]);
            runTest(7, [3, 2, 2]);
            runTest(8, [3, 3, 2]);
            runTest(9, [3, 3, 3]);
            runTest(10, [3, 3, 2, 2]);

            runTest(100, (new Array(32)).fill(3).concat([2, 2]));
            runTest(101, (new Array(33)).fill(3).concat([2]));
            runTest(102, (new Array(34)).fill(3));
        });
    });

    describe('_getWeight', () => {
        let grid;
        const coefficients = {
            normalPhotoSize: 16,
            normalSizeK: 10,
            beautyK: 6,
            sizeDiffK: 100,
            photosInBlockK: 400,
            singlePhotoK: -100000,
            fallbackGridItemK: -1000,
            sameLayoutK: -1000,
            sameLayoutPrevCount: 4
        };
        beforeAll(() => {
            grid = new WowGrid({
                columns: 10,
                items: [],
                coefficients
            });
        });
        const layout1 = [
            [0, 0, 6, 6],
            [6, 0, 4, 8],
            [0, 6, 6, 3],
            [6, 8, 4, 4],
            [0, 9, 6, 3]
        ];
        // -12 = layoutHeight
        // -40 * 10 = normalSizeK * normalSizePart
        // 16 * 100 = sizeDiffK * sizeDiffPart
        // 25 * 400 = photosInBlockK * photosInBlockPart
        const layout1Weight = -12 - 40 * coefficients.normalSizeK + 16 * coefficients.sizeDiffK + 25 * coefficients.photosInBlockK;
        it('should calculate weight for layout', () => {
            expect(grid._getWeight(
                0,
                layout1,
                new Array(5).fill({}),
                null,
                false
            )).toEqual(layout1Weight);
        });
        it('should take beauty if items has', () => {
            expect(grid._getWeight(
                0,
                layout1,
                [{}, { beauty: 1 }, { beauty: -1 }, { beauty: 2 }, { beauty: 0 }],
                null,
                false
            )).toEqual(layout1Weight + (16 - 2) * coefficients.beautyK);
        });
        it('should take singlePhotoK for single photos', () => {
            expect(grid._getWeight(
                0,
                [[0, 0, 10, 10]],
                [{}],
                null,
                false
            )).toEqual(-10 + (-84 * coefficients.normalSizeK) + coefficients.sizeDiffK + coefficients.photosInBlockK + coefficients.singlePhotoK);
        });
        it('should take fallbackGridItemK for fallback layout', () => {
            expect(grid._getWeight(
                0,
                layout1,
                new Array(5).fill({}),
                null,
                true
            )).toEqual(layout1Weight + layout1.length * coefficients.fallbackGridItemK);
        });
        it('should take sameLayoutK for same layout as previous', () => {
            expect(grid._getWeight(
                0,
                layout1,
                new Array(5).fill({}),
                { layout: layout1, height: 12 },
                false
            )).toEqual(layout1Weight + coefficients.sameLayoutPrevCount * coefficients.sameLayoutK);
        });
        it('should take sameLayoutK for same layout as 3 layouts back', () => {
            expect(grid._getWeight(
                0,
                layout1,
                new Array(5).fill({}),
                { height: 1, prev: { height: 1, prev: { layout: layout1, height: 12 } } },
                false
            )).toEqual(layout1Weight + (coefficients.sameLayoutPrevCount - 2) * coefficients.sameLayoutK);
        });
    });

    describe('_getItemByIndex', () => {
        const grid = new WowGrid({
            columns: 10,
            items: []
        });
        grid._layouts = {
            cluster1: {
                layouts: [{
                    indexFrom: 0,
                    layout: ['cluster1item0', 'cluster1item1', 'cluster1item2', 'cluster1item3', 'cluster1item4', 'cluster1item5']
                }, {
                    indexFrom: 6,
                    layout: ['cluster1item6', 'cluster1item7', 'cluster1item8', 'cluster1item9']
                }, {
                    indexFrom: 10,
                    layout: ['cluster1item10']
                }]
            },
            cluster2: {
                layouts: [{
                    indexFrom: 0,
                    layout: ['cluster2item0']
                }]
            }
        };

        it('should return correct item', () => {
            expect(grid._getItemByIndex('cluster1', 0)).toEqual({
                item: 'cluster1item0',
                layoutIndex: 0,
                itemIndexInLayout: 0
            });
            expect(grid._getItemByIndex('cluster1', 1)).toEqual({
                item: 'cluster1item1',
                layoutIndex: 0,
                itemIndexInLayout: 1
            });
            expect(grid._getItemByIndex('cluster1', 2)).toEqual({
                item: 'cluster1item2',
                layoutIndex: 0,
                itemIndexInLayout: 2
            });
            expect(grid._getItemByIndex('cluster1', 3)).toEqual({
                item: 'cluster1item3',
                layoutIndex: 0,
                itemIndexInLayout: 3
            });
            expect(grid._getItemByIndex('cluster1', 4)).toEqual({
                item: 'cluster1item4',
                layoutIndex: 0,
                itemIndexInLayout: 4
            });
            expect(grid._getItemByIndex('cluster1', 5)).toEqual({
                item: 'cluster1item5',
                layoutIndex: 0,
                itemIndexInLayout: 5
            });
            expect(grid._getItemByIndex('cluster1', 6)).toEqual({
                item: 'cluster1item6',
                layoutIndex: 1,
                itemIndexInLayout: 0
            });
            expect(grid._getItemByIndex('cluster1', 7)).toEqual({
                item: 'cluster1item7',
                layoutIndex: 1,
                itemIndexInLayout: 1
            });
            expect(grid._getItemByIndex('cluster1', 8)).toEqual({
                item: 'cluster1item8',
                layoutIndex: 1,
                itemIndexInLayout: 2
            });
            expect(grid._getItemByIndex('cluster1', 9)).toEqual({
                item: 'cluster1item9',
                layoutIndex: 1,
                itemIndexInLayout: 3
            });
            expect(grid._getItemByIndex('cluster1', 10)).toEqual({
                item: 'cluster1item10',
                layoutIndex: 2,
                itemIndexInLayout: 0
            });
            expect(grid._getItemByIndex('cluster2', 0)).toEqual({
                item: 'cluster2item0',
                layoutIndex: 0,
                itemIndexInLayout: 0
            });
        });
    });

    describe('_getClusterLayouts', () => {
        let grid;
        beforeEach(() => {
            grid = new WowGrid({
                columns: 10,
                minWowClusterSize: 9,
                items: []
            });
        });

        it('should generate layouts if they was not generated', () => {
            expect(grid._layouts.clusterId).toBeUndefined();
            grid._getWithoutSizeFallback = () => ([{ size: 9 }]);
            grid._getClusterLayouts({
                id: 'clusterId',
                size: 9
            });
            expect(grid._layouts.clusterId).toEqual({
                isDefault: true,
                size: 9,
                layouts: [{
                    size: 9
                }]
            });
        });

        it('should re-generate layouts if size changed', () => {
            grid._layouts.clusterId = {
                isDefault: true,
                size: 9,
                layouts: [{
                    size: 9
                }]
            };
            grid._getWithoutSizeFallback = () => ([{ size: 5 }, { size: 5 }]);
            grid._getClusterLayouts({
                id: 'clusterId',
                size: 10
            });
            expect(grid._layouts.clusterId).toEqual({
                isDefault: true,
                size: 10,
                layouts: [{
                    size: 5
                }, {
                    size: 5
                }]
            });
        });

        it('should re-generate layouts if isDefault and just got items', () => {
            grid._layouts.clusterId = {
                isDefault: true,
                size: 3
            };

            const items = [horizontalItem, verticalItem, panoramaItem];

            grid._cachedClusterItemStrings.clusterId = 'hv-';
            grid._getClusterLayouts({
                id: 'clusterId',
                size: 3,
                items
            });
            expect(grid._layouts.clusterId).toEqual({
                isDefault: false,
                size: 3,
                itemTypesString: 'hv-',
                items,
                layouts: [{
                    height: 3,
                    indexFrom: 0,
                    layout: [{
                        layoutHeight: 3,
                        layoutWidth: 3,
                        layoutX: 0,
                        layoutY: 0
                    }, {
                        layoutHeight: 3,
                        layoutWidth: 2,
                        layoutX: 3,
                        layoutY: 0
                    }, {
                        layoutHeight: 3,
                        layoutWidth: 4,
                        layoutX: 5,
                        layoutY: 0
                    }]
                }]
            });
        });

        it('should re-generate layouts if size of item changed', () => {
            grid._layouts.clusterId = {
                size: 3,
                itemTypesString: 'hv-'
            };

            const items = [horizontalItem, verticalItem, horizontalItem];

            grid._cachedClusterItemStrings.clusterId = 'hvh';
            grid._getClusterLayouts({
                id: 'clusterId',
                size: 3,
                items
            });

            expect(grid._layouts.clusterId).toEqual({
                isDefault: false,
                size: 3,
                itemTypesString: 'hvh',
                items,
                layouts: [{
                    height: 3,
                    indexFrom: 0,
                    layout: [{
                        layoutHeight: 3,
                        layoutWidth: 3,
                        layoutX: 0,
                        layoutY: 0
                    }, {
                        layoutHeight: 3,
                        layoutWidth: 2,
                        layoutX: 3,
                        layoutY: 0
                    }, {
                        layoutHeight: 3,
                        layoutWidth: 3,
                        layoutX: 5,
                        layoutY: 0
                    }]
                }]
            });
        });

        it('should not re-generate layouts if still default layout and size not changed', () => {
            const initialLayouts = {
                isDefault: true,
                size: 9,
                layouts: [{
                    size: 9
                }]
            };
            grid._layouts.clusterId = initialLayouts;
            grid._getClusterLayouts({
                id: 'clusterId',
                size: 9
            });
            expect(grid._layouts.clusterId).toBe(initialLayouts);
        });

        it('should not re-generate layouts if items sizes not changed', () => {
            const initialLayouts = {
                size: 3,
                itemTypesString: 'hv-'
            };
            grid._layouts.clusterId = initialLayouts;
            grid._cachedClusterItemStrings.clusterId = 'hv-';
            grid._getClusterLayouts({
                id: 'clusterId',
                size: 3,
                items: [horizontalItem, verticalItem, panoramaItem]
            });
            expect(grid._layouts.clusterId).toBe(initialLayouts);
        });

        describe('appendItems', () => {
            beforeEach(() => {
                grid._options.minWowClusterSize = 3;
                grid._calculateBestLayoutsForItems = jest.fn().mockReturnValue([]);
            });

            it('if new items start with old items and appended items count is enough => append', () => {
                const initalItems = [{ data: 'superman' }, { data: 'batman' }, { data: 'ironman' }];

                grid._calculateBestLayoutsForItems.mockImplementation((items) => [{
                    indexFrom: 0,
                    layout: items
                }]);

                grid._layouts.clusterId = {
                    size: 3,
                    items: initalItems,
                    layouts: [
                        { items: initalItems }
                    ]
                };

                const addedItems = [{ data: 'aquaman' }, { data: 'hulk' }, { data: 'thanos' }];

                grid._getClusterLayouts({
                    id: 'clusterId',
                    size: 6,
                    items: [...initalItems, ...addedItems]
                });

                // Проверяем, что расчет раскладок вызван только у добавленных элементов
                expect(grid._calculateBestLayoutsForItems).toHaveBeenCalledWith(addedItems);

                expect(grid._layouts.clusterId).toEqual({
                    size: 6,
                    items: [...initalItems, ...addedItems],
                    layouts: [
                        { items: initalItems },
                        { indexFrom: 3, layout: addedItems }
                    ]
                });
            });

            it('if new items start with old items but count of appended items is not enough => rebuild all', () => {
                const initalItems = [{ data: 'superman' }, { data: 'batman' }, { data: 'ironman' }];

                grid._layouts.clusterId = {
                    size: 3,
                    items: initalItems,
                    layouts: [
                        { items: initalItems }
                    ]
                };

                // Добавляем только один элемент, а для кластера указан минимум 3 в опциях грида
                const addedItems = [{ data: 'aquaman' }];

                grid._getClusterLayouts({
                    id: 'clusterId',
                    size: 4,
                    items: [...initalItems, ...addedItems]
                });

                // Проверяем, что расчет раскладок вызван для всех элементов
                expect(grid._calculateBestLayoutsForItems).toHaveBeenCalledWith([...initalItems, ...addedItems]);
            });

            it('if new cluster => not append, rebuild whole cluster', () => {
                const items = [{ data: 'superman' }, { data: 'batman' }, { data: 'ironman' }];

                grid._layouts.clusterId = {
                    items: []
                };

                grid._getClusterLayouts({
                    id: 'clusterId2',
                    size: 4,
                    items
                });

                // Проверяем, что расчет раскладок вызван для всех элементов
                expect(grid._calculateBestLayoutsForItems).toHaveBeenCalledWith(items);
            });

            it('if new items not start with old items => not append, rebuild whole cluster', () => {
                const items = [{ data: 'superman' }, { data: 'batman' }, { data: 'ironman' }];

                grid._layouts.clusterId = {
                    items
                };

                const newItems = [
                    { data: 'robin hood' },
                    ...items,
                    { data: 'aquaman' }, { data: 'hulk' }, { data: 'thanos' }
                ];

                grid._getClusterLayouts({
                    id: 'clusterId',
                    size: newItems.length,
                    items: newItems
                });

                // Проверяем, что расчет раскладок вызван для всех элементов
                expect(grid._calculateBestLayoutsForItems).toHaveBeenCalledWith(newItems);
            });
        });
    });
});
