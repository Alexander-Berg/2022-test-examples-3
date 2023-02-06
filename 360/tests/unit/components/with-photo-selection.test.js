import _ from 'lodash';
import { constant, noop } from 'lodash';
import { mount } from 'enzyme';
import React, { Component } from 'react';
import withPhotoSelection, { ClusterSelectionState } from '../../../components/redux/components/photo2/with-resources-selection';

class WrappedComponent extends Component {
    render() {
        return <div />;
    }
}

describe('WithPhotoSelection', () => {
    describe('одиночное выделение', () => {
        // FIXME не проходит проверку, если вообще не осталось кластеров
        const tests = [
            {
                title: 'не должно измениться, если не изменилась струтура кластера',
                dataBefore: [1, 2, 1],
                dataAfter: [1, 2, 1],
                index: 1,
                isSelectionUnchanged: true
            },
            {
                title: 'с id ресурса должно исчезнуть, если находилось в начале кластера, а потом ресурс удалился',
                dataBefore: [1, 2, 1],
                dataAfter: [1, [1, true, [0]], 1],
                index: 1
            },
            {
                title: 'с id ресурса должно исчезнуть, если находилось в конце кластера, а потом ресурс удалился',
                dataBefore: [1, 2, 1],
                dataAfter: [1, 1, 1],
                index: 2
            },
            {
                title: 'с id ресурса должно исчезнуть, если находилось в середине кластера, а потом ресурс удалился',
                dataBefore: [1, 3, 1],
                dataAfter: [1, [2, true, [1]], 1],
                index: 2
            },
            {
                title: 'с id ресурса должно исчезнуть, если кластер был удален',
                dataBefore: [1, 3, 1],
                dataAfter: [1, null, 1],
                index: 2
            },
            {
                title: 'без id ресурса должно исчезнуть, если кластер меньше по размеру',
                dataBefore: [1, 2, 1],
                dataAfter: [1, 1, 1],
                index: 2
            },
            {
                title: 'должно переместиться, если кластеры перед выделением ужались',
                dataBefore: [2, 2, 1],
                dataAfter: [1, 2, 1],
                index: 3,
                isSelectionUnchanged: true
            },
            {
                title: 'должно переместиться, если кластеры перед выделением расширились',
                dataBefore: [2, 2, 1],
                dataAfter: [3, 2, 1],
                index: 3,
                isSelectionUnchanged: true
            },
            {
                title: 'не должно переместиться, если кластеры перед выделением изменились, но отступ не изменился',
                dataBefore: [2, null, 2, 1],
                dataAfter: [1, 1, 2, 1],
                index: 3,
                isSelectionUnchanged: true
            },
            {
                title: 'не должно переместиться, если изменились кластера после выделения',
                dataBefore: [2, 2, 1],
                dataAfter: [2, 2, 2, 15],
                index: 3,
                isSelectionUnchanged: true
            }
        ];

        tests.forEach(({ title, dataBefore, dataAfter, index, isSelectionUnchanged }) => {
            it(title, () => {
                const before = createTestData(dataBefore);
                const after = createTestData(dataAfter);
                const onSelectionChanged = jest.fn();

                const propsBefore = {
                    onSelectionChanged,
                    selectedResources: [getResourceIdForIndex(before.clusters, index)],
                    OSFamily: 'MacOS',
                    structureVersion: 0,
                    resourcesVersion: 0,
                    items: before.clusters,
                    isSelectableResource: constant(true),
                    fetchClustersRanges: noop,
                    getResourceById: (id) => before.resources[id],
                    doesMatchFilter: constant(true)
                };

                const propsAfter = Object.assign({}, propsBefore, {
                    structureVersion: 1,
                    items: after.clusters,
                    getResourceById: (id) => after.resources[id],
                });

                const TestComponent = withPhotoSelection(WrappedComponent);
                const testWrapper = mount(<TestComponent {...propsBefore} />);

                testWrapper.setProps(propsAfter);

                if (isSelectionUnchanged) {
                    expect(propsAfter.selectedResources).toEqual(onSelectionChanged.mock.calls[0][0].resources);
                } else {
                    expect(onSelectionChanged.mock.calls[0][0].resources).toEqual([]);
                }
            });
        });
    });

    describe('Множественное выделение', () => {
        const tests = [
            {
                title: 'с обоими id ресурсов должно сохраниться, если структура кластеров не изменилась',
                dataBefore: [1, 2, 1],
                dataAfter: [1, 2, 1],
                indicesBefore: [0, 3],
                indicesAfter: [0, 3]
            },
            {
                title: 'без id ресурсов должно сохраниться, если структура кластеров не изменилась',
                dataBefore: [[1, false], [2, false], [1, false]],
                dataAfter: [[1, false], [2, false], [1, false]],
                indicesBefore: [0, 3],
                indicesAfter: [0, 3]
            },
            {
                title: 'с id ресурсов должно обновиться, если структура кластеров изменилась, но id ресурсов остались',
                dataBefore: [2, 3, null, 4, [2, true, [0]], 2],
                dataAfter: [1, [2, true, [0]], 5, null, 3, 5],
                indicesBefore: [3, 9],
                indicesAfter: [1, 9]
            },
            {
                title: 'без id ресурсов должно обновиться, если структура кластеров изменилась, но IndexPath якорей сохранились',
                dataBefore: [[2, false], [3, false], null, [4, false], [2, false], [2, false]],
                dataAfter: [[1, false], [2, false], [5, false], null, [3, false], [5, false]],
                indicesBefore: [3, 9],
                indicesAfter: [2, 8]
            },
            {
                title: 'с startId должно обновиться, если структура кластеров изменилась, но якоря остались на месте',
                dataBefore: [2, 3, null, 4, [2, false], 2],
                dataAfter: [[1, false], [2, true, [0]], [5, false], null, 3, [5, false]],
                indicesBefore: [3, 9],
                indicesAfter: [1, 8]
            },
            {
                title: 'с endId должно обновиться, если структура кластеров изменилась, но якоря остались на месте',
                dataBefore: [2, [2, false], null, 4, [3, true, [0]], 2],
                dataAfter: [1, [2, true, [0]], [5, false], null, 3, [5, false]],
                indicesBefore: [3, 8],
                indicesAfter: [2, 9]
            },
            {
                title: 'с id ресурсов должно уменьшиться, если исчез startId, но еще есть следующие ресурсы в новом кластере',
                dataBefore: [2, 3, 3, 2],
                dataAfter: [2, [2, true, [1]], 3, 2],
                indicesBefore: [3, 8],
                indicesAfter: [3, 7]
            },
            {
                title: 'с id ресурсов должно уменьшиться, если исчез endId, но еще есть предыдущие ресурсы в новом кластере',
                dataBefore: [2, 3, 3, 2],
                dataAfter: [2, 3, [2, true, [1]], 2],
                indicesBefore: [3, 6],
                indicesAfter: [3, 5]
            },
            {
                title: 'в разных кластерах с id ресурсов должно уменьшиться, если исчез startId и endId, но в кластерах можно найти новые якоря',
                dataBefore: [2, 3, 3, 2],
                dataAfter: [2, [2, true, [1]], [2, true, [1]], 2],
                indicesBefore: [3, 6],
                indicesAfter: [3, 4]
            },
            {
                title: 'в одном кластере с id ресурсов должно уменьшиться, если исчез startId и endId, но в кластере можно найти якоря',
                dataBefore: [2, 5, 2],
                dataAfter: [2, [3, true, [1, 3]], 2],
                indicesBefore: [3, 5],
                indicesAfter: [3, 3]
            },
            {
                title: 'в разных кластерах с id ресурсов должно уменьшиться, если исчез startId, и в текущем кластере найти якорь невозможно',
                dataBefore: [3, 2],
                dataAfter: [1, 2],
                indicesBefore: [1, 3],
                indicesAfter: [1, 1]
            },
            {
                title: 'в разных кластерах с id ресурсов должно уменьшиться, если исчез startId, и в текущем кластере найти якорь невозможно, и следующего кластера нет',
                dataBefore: [null, 3, 1, 2],
                dataAfter: [2, 1, null, 2],
                indicesBefore: [1, 4],
                indicesAfter: [3, 3]
            },
            {
                title: 'в одном кластере с id ресурсов должно исчезнуть, если исчез startId, а следующего кластера нет',
                dataBefore: [3],
                dataAfter: [1],
                indicesBefore: [1, 2]
            },
            {
                title: 'в разных кластерах с id ресурсов должно уменьшиться, если исчез endId, и в текущем кластере найти якорь невозможно',
                dataBefore: [2, 3],
                dataAfter: [2, [1, true, [0, 1]]],
                indicesBefore: [1, 3],
                indicesAfter: [1, 1]
            },
            {
                title: 'в разных кластерах с id ресурсов должно уменьшиться, если исчез endId, и в текущем кластере найти якорь невозможно, и предыдущего кластера нет',
                dataBefore: [2, null, null, 1, 3],
                dataAfter: [2, 2, 3, null, [1, true, [0, 1]]],
                indicesBefore: [1, 4],
                indicesAfter: [1, 6]
            },
            {
                title: 'в одном кластере с id ресурсов должно исчезнуть, если исчез endId, и предыдущего кластера нет',
                dataBefore: [3],
                dataAfter: [[1, true, [0, 1]]],
                indicesBefore: [0, 1]
            },
            {
                title: 'в разных кластерах с id ресурсов должно уменьшиться, если исчез startId и endId, и в текущих кластерах найти якоря невозможно',
                dataBefore: [2, 3, 2],
                dataAfter: [1, 3, [1, true, [0]]],
                indicesBefore: [1, 5],
                indicesAfter: [1, 3]
            },
            {
                title: 'в разных кластерах без startId должно перепрыгнуть на следующий кластер, если в текущем больше нет IndexPath',
                dataBefore: [[2, false], 3, 2],
                dataAfter: [1, 3, 2],
                indicesBefore: [1, 5],
                indicesAfter: [1, 4]
            },
            {
                title: 'в разных кластерах без endId и startId должен перепрыгнуть, если в текущих кластерах больше нет IndexPath',
                dataBefore: [[2, false], 3, [2, false]],
                dataAfter: [1, 3, 1],
                indicesBefore: [1, 6],
                indicesAfter: [1, 4]
            },
            {
                title: 'в разных кластерах без startId и endId не должно перепрыгивать на другие кластера, если в текущих есть нужные IndexPath',
                dataBefore: [[3, false], 3, [3, false]],
                dataAfter: [2, 3, 2],
                indicesBefore: [1, 7],
                indicesAfter: [1, 6]
            },
            {
                title: 'в разных кластерах с startId должно уменьшится, если кластера с startClusterId больше нет',
                dataBefore: [1, 2, 3],
                dataAfter: [null, null, 2],
                indicesBefore: [0, 4],
                indicesAfter: [0, 1]
            },
            {
                title: 'в разных кластерах без startId должно уменьшится, если кластера с startClusterId больше нет',
                dataBefore: [[1, false], [2, false], [3, false]],
                dataAfter: [null, null, 2],
                indicesBefore: [0, 4],
                indicesAfter: [0, 1]
            },
            {
                title: 'в разных кластерах с endId должно уменьшится, если кластера с endClusterId больше нет',
                dataBefore: [1, 2, 3],
                dataAfter: [1, 2, null, 3],
                indicesBefore: [0, 3],
                indicesAfter: [0, 2]
            },
            {
                title: 'в разных кластерах без endId должно уменьшится, если кластера с endClusterId больше нет',
                dataBefore: [[1, false], [2, false], [3, false]],
                dataAfter: [[1, false], [2, false], null, [3, false]],
                indicesBefore: [0, 3],
                indicesAfter: [0, 2]
            },
            {
                title: 'в разных кластерах c startId и endId должно удалиться, если нет промежуточных кластеров и нет ресурсов',
                dataBefore: [2, 2, 2, 2, 2],
                dataAfter: [2, 1, null, [1, true, [0]], 2],
                indicesBefore: [3, 6]
            },
            {
                title: 'в разных кластерах c startId и endId должно удалиться, если нет промежуточных кластеров и нет ресурсов',
                dataBefore: [[2, false], [2, false], [2, false], [2, false], [2, false]],
                dataAfter: [2, 1, null, null, 2],
                indicesBefore: [3, 6]
            }
        ];

        tests.forEach(({ title, dataBefore, dataAfter, indicesBefore, indicesAfter = [] }) => {
            it(title, () => {
                class WrappedComponent extends Component {
                    selectResourceAtIndex(index, withShift = false) {
                        const indexPath = getIndexPathForIndex(this.props.items, index);
                        const event = withShift ? { shiftKey: true } : {};

                        this.props.onSelectResource(indexPath, false, event);
                    }

                    render() {
                        return <div />;
                    }
                }

                const onSelectionChanged = jest.fn();

                const stateBefore = createTestData(dataBefore);
                const stateAfter = createTestData(dataAfter);
                const selectedResourcesBefore = getResourcesIdsForInterval(stateBefore.clusters, indicesBefore);
                const selectedResourcesAfter = getResourcesIdsForInterval(stateAfter.clusters, indicesAfter);

                const propsBefore = {
                    onSelectionChanged,
                    selectedResources: [],
                    OSFamily: 'MacOS',
                    structureVersion: 0,
                    resourcesVersion: 0,
                    items: stateBefore.clusters,
                    isSelectableResource: constant(true),
                    fetchClustersRanges: noop,
                    getResourceById: (id) => stateBefore.resources[id],
                    doesMatchFilter: constant(true)
                };

                const TestComponent = withPhotoSelection(WrappedComponent);
                const testWrapper = mount(<TestComponent {...propsBefore} />);
                const wrappedInstance = testWrapper.find(WrappedComponent).instance();

                wrappedInstance.selectResourceAtIndex(indicesBefore[0]);
                wrappedInstance.selectResourceAtIndex(indicesBefore[1], true);

                const selectedResources = _.last(onSelectionChanged.mock.calls)[0].resources;
                expect(selectedResources).toEqual(selectedResourcesBefore);

                const propsAfter = Object.assign({}, propsBefore, {
                    selectedResources: selectedResources,
                    structureVersion: 1,
                    items: stateAfter.clusters,
                    getResourceById: (id) => stateAfter.resources[id],
                });

                testWrapper.setProps(propsAfter);

                expect(_.last(onSelectionChanged.mock.calls)[0].resources).toEqual(selectedResourcesAfter);
            });
        });
    });

    describe('Выделение кластера', () => {
        class WrappedComponent extends Component {
            selectResourceAtIndex(index, withShift = false) {
                const indexPath = getIndexPathForIndex(this.props.items, index);
                const event = withShift ? { shiftKey: true } : {};

                this.props.onSelectResource(indexPath, false, event);
            }

            selectClusterAtIndex(clusterIndex) {
                this.props.onSelectClusters([this.props.items[clusterIndex].id], ClusterSelectionState.NONE);
            }

            deselectClusterAtIndex(clusterIndex) {
                this.props.onSelectClusters([this.props.items[clusterIndex].id], ClusterSelectionState.ALL);
            }

            render() {
                return <div />;
            }
        }

        it('должно корректно выделить и развыделить кластер', () => {
            const onSelectionChanged = jest.fn();

            const state = createTestData([1, 2, 1]);

            const props = {
                onSelectionChanged,
                selectedResources: [],
                OSFamily: 'MacOS',
                structureVersion: 0,
                resourcesVersion: 0,
                items: state.clusters,
                isSelectableResource: constant(true),
                fetchClustersRanges: noop,
                getResourceById: (id) => state.resources[id],
                doesMatchFilter: constant(true)
            };

            const TestComponent = withPhotoSelection(WrappedComponent);
            const testWrapper = mount(<TestComponent {...props} />);
            const wrappedInstance = testWrapper.find(WrappedComponent).instance();

            wrappedInstance.selectClusterAtIndex(1);

            expect(_.last(onSelectionChanged.mock.calls)[0]).toEqual({
                resources: getResourcesIdsForClusters(state.clusters, 1),
                total: 2, missing: 0, unfilteredTotal: 2
            });

            wrappedInstance.deselectClusterAtIndex(1);

            expect(_.last(onSelectionChanged.mock.calls)[0]).toEqual({
                resources: [], total: 0, missing: 0, unfilteredTotal: 0
            });
        });

        it('должно корректно выделить и развыделить несколько несмежных кластеров', () => {
            const onSelectionChanged = jest.fn();

            const state = createTestData([1, 2, 1, 2, 3]);

            const props = {
                onSelectionChanged,
                selectedResources: [],
                OSFamily: 'MacOS',
                structureVersion: 0,
                resourcesVersion: 0,
                items: state.clusters,
                isSelectableResource: constant(true),
                fetchClustersRanges: noop,
                getResourceById: (id) => state.resources[id],
                doesMatchFilter: constant(true)
            };

            const TestComponent = withPhotoSelection(WrappedComponent);
            const testWrapper = mount(<TestComponent {...props} />);
            const wrappedInstance = testWrapper.find(WrappedComponent).instance();

            wrappedInstance.selectClusterAtIndex(1);
            wrappedInstance.selectClusterAtIndex(3);

            expect(_.last(onSelectionChanged.mock.calls)[0]).toEqual({
                resources: getResourcesIdsForClusters(state.clusters, [1, 3]),
                total: 4, missing: 0, unfilteredTotal: 4
            });

            wrappedInstance.deselectClusterAtIndex(1);

            expect(_.last(onSelectionChanged.mock.calls)[0]).toEqual({
                resources: getResourcesIdsForClusters(state.clusters, 3),
                total: 2, missing: 0, unfilteredTotal: 2
            });

            wrappedInstance.deselectClusterAtIndex(3);

            expect(_.last(onSelectionChanged.mock.calls)[0]).toEqual({
                resources: [], total: 0, missing: 0, unfilteredTotal: 0
            });
        });

        it('должно корректно выделить и развыделить два смежных кластера', () => {
            const onSelectionChanged = jest.fn();

            const state = createTestData([1, 2, 1, 2, 3]);

            const props = {
                onSelectionChanged,
                selectedResources: [],
                OSFamily: 'MacOS',
                structureVersion: 0,
                resourcesVersion: 0,
                items: state.clusters,
                isSelectableResource: constant(true),
                fetchClustersRanges: noop,
                getResourceById: (id) => state.resources[id],
                doesMatchFilter: constant(true)
            };

            const TestComponent = withPhotoSelection(WrappedComponent);
            const testWrapper = mount(<TestComponent {...props} />);
            const wrappedInstance = testWrapper.find(WrappedComponent).instance();

            wrappedInstance.selectClusterAtIndex(2);
            wrappedInstance.selectClusterAtIndex(1);

            expect(_.last(onSelectionChanged.mock.calls)[0]).toEqual({
                resources: getResourcesIdsForClusters(state.clusters, [1, 2]),
                total: 3, missing: 0, unfilteredTotal: 3
            });

            wrappedInstance.deselectClusterAtIndex(1);

            expect(_.last(onSelectionChanged.mock.calls)[0]).toEqual({
                resources: getResourcesIdsForClusters(state.clusters, 2),
                total: 1, missing: 0, unfilteredTotal: 1
            });

            wrappedInstance.selectClusterAtIndex(3);

            expect(_.last(onSelectionChanged.mock.calls)[0]).toEqual({
                resources: getResourcesIdsForClusters(state.clusters, [2, 3]),
                total: 3, missing: 0, unfilteredTotal: 3
            });

            wrappedInstance.deselectClusterAtIndex(2);

            expect(_.last(onSelectionChanged.mock.calls)[0]).toEqual({
                resources: getResourcesIdsForClusters(state.clusters, 3),
                total: 2, missing: 0, unfilteredTotal: 2
            });
        });

        it('должно корректно выделить и развыделить кластер, пересекающий другое выделение', () => {
            const onSelectionChanged = jest.fn();

            const state = createTestData([2, 3, 2]);

            const props = {
                onSelectionChanged,
                selectedResources: [],
                OSFamily: 'MacOS',
                structureVersion: 0,
                resourcesVersion: 0,
                items: state.clusters,
                isSelectableResource: constant(true),
                fetchClustersRanges: noop,
                getResourceById: (id) => state.resources[id],
                doesMatchFilter: constant(true)
            };

            const TestComponent = withPhotoSelection(WrappedComponent);
            const testWrapper = mount(<TestComponent {...props} />);
            const wrappedInstance = testWrapper.find(WrappedComponent).instance();

            wrappedInstance.selectResourceAtIndex(1);
            wrappedInstance.selectResourceAtIndex(2, true);

            expect(_.last(onSelectionChanged.mock.calls)[0]).toEqual({
                resources: getResourcesIdsForInterval(state.clusters, [1, 2]),
                total: 2, missing: 0, unfilteredTotal: 2
            });

            wrappedInstance.selectClusterAtIndex(1);

            expect(_.last(onSelectionChanged.mock.calls)[0]).toEqual({
                resources: getResourcesIdsForInterval(state.clusters, [1, 4]),
                total: 4, missing: 0, unfilteredTotal: 4
            });

            testWrapper.setProps({
                selectedResources: [],
                resourcesVersion: 1
            });

            expect(_.last(onSelectionChanged.mock.calls)[0]).toEqual({
                resources: [], total: 0, missing: 0, unfilteredTotal: 0
            });

            wrappedInstance.selectResourceAtIndex(4);
            wrappedInstance.selectResourceAtIndex(5, true);

            expect(_.last(onSelectionChanged.mock.calls)[0]).toEqual({
                resources: getResourcesIdsForInterval(state.clusters, [4, 5]),
                total: 2, missing: 0, unfilteredTotal: 2
            });

            wrappedInstance.selectClusterAtIndex(1);

            expect(_.last(onSelectionChanged.mock.calls)[0]).toEqual({
                resources: getResourcesIdsForInterval(state.clusters, [2, 5]),
                total: 4, missing: 0, unfilteredTotal: 4
            });
        });

        it('должно корректно выделить и развыделить кластер между двумя другими выделенными кластерами и смежный с одним из них', () => {
            const onSelectionChanged = jest.fn();

            const state = createTestData([2, 2, 2, 2]);

            const props = {
                onSelectionChanged,
                selectedResources: [],
                OSFamily: 'MacOS',
                structureVersion: 0,
                resourcesVersion: 0,
                items: state.clusters,
                isSelectableResource: constant(true),
                fetchClustersRanges: noop,
                getResourceById: (id) => state.resources[id],
                doesMatchFilter: constant(true)
            };

            const TestComponent = withPhotoSelection(WrappedComponent);
            const testWrapper = mount(<TestComponent {...props} />);
            const wrappedInstance = testWrapper.find(WrappedComponent).instance();

            wrappedInstance.selectClusterAtIndex(0);
            wrappedInstance.selectClusterAtIndex(3);

            expect(_.last(onSelectionChanged.mock.calls)[0]).toEqual({
                resources: getResourcesIdsForClusters(state.clusters, [0, 3]),
                total: 4, missing: 0, unfilteredTotal: 4
            });

            wrappedInstance.selectClusterAtIndex(1);

            expect(_.last(onSelectionChanged.mock.calls)[0]).toEqual({
                resources: getResourcesIdsForClusters(state.clusters, [0, 1, 3]),
                total: 6, missing: 0, unfilteredTotal: 6
            });

            wrappedInstance.deselectClusterAtIndex(1);

            expect(_.last(onSelectionChanged.mock.calls)[0]).toEqual({
                resources: getResourcesIdsForClusters(state.clusters, [0, 3]),
                total: 4, missing: 0, unfilteredTotal: 4
            });

            wrappedInstance.selectClusterAtIndex(2);

            expect(_.last(onSelectionChanged.mock.calls)[0]).toEqual({
                resources: getResourcesIdsForClusters(state.clusters, [0, 2, 3]),
                total: 6, missing: 0, unfilteredTotal: 6
            });

            wrappedInstance.deselectClusterAtIndex(2);

            expect(_.last(onSelectionChanged.mock.calls)[0]).toEqual({
                resources: getResourcesIdsForClusters(state.clusters, [0, 3]),
                total: 4, missing: 0, unfilteredTotal: 4
            });
        });
    });

    it('Должен корректно работать при одновременном снятии выделения и удалении ресурса из кластера', () => {
        const resources = {
            '/disk/1.jpg': {
                id: '/disk/1.jpg',
                meta: { photoslice_time: 1576605042 }
            },
            '/disk/2.jpg': {
                id: '/disk/2.jpg',
                meta: { photoslice_time: 1576605034 }
            },
            '/disk/3.jpg': {
                id: '/disk/3.jpg',
                meta: { photoslice_time: 1576605023 }
            }
        };
        const itemsBefore = [{
            id: '0000001576605023000_0000001576605042000',
            from: 1576605023000,
            to: 1576605042000,
            items: [
                { id: '/disk/1.jpg', itemId: '2_0000001576605042000_2f18eef525719c084f7e249da632f5f86ee16cd3f0a1da9c271deafb3e03f340_1576767929687310' },
                { id: '/disk/2.jpg', itemId: '2_0000001576605034000_486fdb531115b74560b8cb18eae481968459e93dbf0cfaf215920db0c8370563_1576767929729923' },
                { id: '/disk/3.jpg', itemId: '2_0000001576605023000_5814294a6be2313eeeb9fc8cc6aad4349436d2c31426353312654b46bc8ec793_1576767929656966' }
            ]
        }];

        const onSelectionChanged = jest.fn();

        const TestComponent = withPhotoSelection(WrappedComponent);
        const testWrapper = mount(
            <TestComponent
                items={itemsBefore}
                selectedResources={['/disk/1.jpg', '/disk/2.jpg', '/disk/3.jpg']}
                structureVersion={0}
                resourcesVersion={0}
                OSFamily="MacOS"
                fetchClustersRanges={noop}
                isSelectableResource={constant(true)}
                doesMatchFilter={constant(true)}
                getResourceById={(id) => resources[id] }
                onSelectionChanged={onSelectionChanged}
            />
        );

        testWrapper.setProps({
            selectedResources: ['/disk/3.jpg']
        });

        const itemsAfter = [Object.assign({}, itemsBefore[0], { items: itemsBefore[0].items.slice(0, 1) })];
        testWrapper.setProps({
            items: itemsAfter,
            structureVersion: 1
        });

        expect(onSelectionChanged).toBeCalledWith({ resources: [], total: 0, missing: 0, unfilteredTotal: 0 });
    });
});

/**
 * @typedef {number} TestClusterItemsCount
 * @typedef {boolean} TestClusterLoadFlag
 * @typedef {Array.<number>} TestClusterMissingItems
 *
 * @typedef {number} SimpleTestCluster
 * @typedef {[TestClusterItemsCount, TestClusterLoadFlag, ?TestClusterMissingItems]} CompositeTestCluster
 *
 * @typedef {SimpleTestCluster|CompositeTestCluster} TestCluster
 *
 * @typedef {Object} Resource
 * @property {string} id
 * @property {Object} meta
 * @property {number} meta.photoslice_time
 *
 * @typedef {Object} TestData
 * @property {Object.<string, Resource>} resources
 * @property {Array.<import("../../../components/redux/store/actions/photo").Cluster>} clusters
 */

/**
 * @description Генерирует кластеры и ресуры, необходимы для теста.
 * Кластер можно описать несколькими способами:
 * - `SimpleTestCluster` — это количество фоток в кластере; т.о.
 *   createTestData([1]) создаст один кластер
 *   {
 *     "clusters": [{
 *       "id": "cluster0",
 *       "size": 1,
 *       "to": 100000,
 *       "from": 91000,
 *       "items": [{
 *         "id": "cluster0resource0"
 *       }]
 *     }],
 *     "resources": {
 *       "cluster0resource0": {
 *         "id": "cluster0resource0",
 *         "meta": {
 *           "photoslice_time": 100
 *         }
 *       }
 *     }
 *   }
 * - `CompositeTestCluster` регулирует загруженность и состав кластера:
 *   - кол-во фоток;
 *   - пропущенные фотки;
 *   - загруженность кластера
 *   [1, true] равнозначен 1: { id: 'cluster0', size: 1, items: [{ id: 'cluster0resource0' }] }
 *   [1, false] кластер не загружен: { id: '', size: 1 }
 *   [2, true, [0, 2]] кластер загружен, но нет двух ресурсов
 *     { id: 'cluster0', size: 2, items: [{ id: 'cluster0resource1' }, { id: 'cluster0resource3' }] }
 *   null кластер пропущен, например, [[1, false], null, [1, false]] превратится в
 *     [{ id: 'cluster0', size: 1 }, { id: 'cluster2', size: 1 }]
 *
 * @param {Array.<TestCluster|null>} spec
 * @param {number} maxClusters
 * @param {number} maxItemsInCluster
 *
 * @returns {TestData}
 */
function createTestData(spec, maxClusters = 10, maxItemsInCluster = 10) {
    const lastClusterTo = maxClusters * maxItemsInCluster;

    const resources = {};

    const clusters = spec.map((clusterSpec, clusterIndex) => {
        let clusterSize = 0;
        let clusterLoaded = true;
        let excludedItemsIndices = [];

        const to = lastClusterTo - (clusterIndex * maxItemsInCluster);
        const from = to - maxItemsInCluster + 1;

        if (clusterSpec === null) {
            return null;
        }

        if (Array.isArray(clusterSpec)) {
            clusterSize = clusterSpec[0];
            clusterLoaded = clusterSpec[1];
            excludedItemsIndices = clusterSpec[2] || [];
        } else {
            clusterSize = clusterSpec;
        }

        const clusterId = `cluster${clusterIndex}`;

        const cluster = {
            id: clusterId,
            size: clusterSize,
            to: to * 1000,
            from: from * 1000
        };

        if (clusterLoaded) {
            cluster.items = [];

            let id = 0;

            while (cluster.items.length < clusterSize) {
                if (!excludedItemsIndices.includes(id)) {
                    const resourceId = `${clusterId}resource${id}`;
                    const photosliceTime = to - id;

                    const resource = {
                        id: resourceId,
                        meta: { photoslice_time: photosliceTime }
                    };

                    cluster.items.push(_.omit(resource, 'meta'));
                    resources[resourceId] = resource;
                }

                id++;
            }
        } else {
            let id = 0;
            let index = 0;

            while (index < clusterSize) {
                if (!excludedItemsIndices.includes(id)) {
                    index++;
                }

                id++;
            }
        }

        return cluster;
    }).filter(Boolean);

    return { clusters, resources };
}

/**
 * @param {Array.<import("../../../components/redux/store/actions/photo").Cluster>} clusters
 * @param {number} index
 *
 * @returns {?import("../../../components/redux/store/actions/photo").IndexPath}
 */
function getIndexPathForIndex(clusters, index) {
    let offset = 0;

    for (let clusterIndex = 0; clusterIndex < clusters.length; clusterIndex++) {
        const cluster = clusters[clusterIndex];

        if (index >= offset && index < offset + cluster.size) {
            return { clusterIndex, resourceIndex: index - offset };
        }

        offset += cluster.size;
    }
}

/**
 * @param {Array.<import("../../../components/redux/store/actions/photo").Cluster>} clusters
 * @param {number} index
 *
 * @returns {?string}
 */
function getResourceIdForIndex(clusters, index) {
    const indexPath = getIndexPathForIndex(clusters, index);

    if (indexPath) {
        return clusters[indexPath.clusterIndex].items[indexPath.resourceIndex].id;
    }
}

/**
 * @param {Array.<import("../../../components/redux/store/actions/photo").Cluster>} clusters
 * @param {[number, number]} interval
 *
 * @returns {Array.<string>}
 */
function getResourcesIdsForInterval(clusters, [start, end]) {
    const resources = clusters.reduce((resources, { items, size }) => {
        if (items) {
            return resources.concat(items.map(_.property('id')));
        }

        return resources.concat(new Array(size));
    }, []);

    return resources.slice(start, end + 1).filter(Boolean);
}

/**
 * @param {Array.<import("../../../components/redux/store/actions/photo").Cluster>} clusters
 * @param {number|Array.<number>} indices
 *
 * @returns {Array.<string>}
 */
function getResourcesIdsForClusters(clusters, indices) {
    const clusterIndices = [].concat(indices);

    return clusters.reduce((resources, cluster, clusterIndex) => {
        if (clusterIndices.includes(clusterIndex)) {
            return resources.concat(_.get(cluster, 'items', []).map(_.property('id')));
        }

        return resources;
    }, []);
}
