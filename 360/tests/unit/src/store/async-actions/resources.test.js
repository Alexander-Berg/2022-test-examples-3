import {
    fetchNextPortion,
    setCurrentResource,
    checkLoadListingForResource,
    checkLoadListingForSlider,
    fetchDirSize,
    resetListPortionsPromises
} from '../../../../../src/store/async-actions/resources';
import { updateResources, setSelectedResources, setUrl, _openSlider } from '../../../../../src/store/actions';
import getStore from '../index';

import getFixture from '../../../../fixtures';

const testDirectoryFixture = getFixture({
    type: 'public_info',
    params: { hash: '/+dWODt1e4HskzD1QFVevBqJzbZ7bbHr4F3iI95rlqk=' },
    formatted: true
});

const testDirectoryId = 'e574d5e59a3452adc6ed51bb5488f07876c5bbbe101940956ab628efea3974a7';
const testDirectoryFirstPortionFixture = getFixture({
    type: 'public_list',
    params: { hash: '/+dWODt1e4HskzD1QFVevBqJzbZ7bbHr4F3iI95rlqk=' },
    formatted: true
});
const testSubDirectoryId = '91db7945c7664df88a65ab97767c56ff58327bc8c5354a648622c39f4f407e8f';
const testSubDirectoryFirstPortion = getFixture({
    type: 'public_list',
    params: { hash: '/+dWODt1e4HskzD1QFVevBqJzbZ7bbHr4F3iI95rlqk=000' },
    formatted: true
});

const testDirectoryWithBlockedId = 'aaa4d5e59a3452adc6ed51bb5488f07876c5bbbe101940956ab628efea3974a7';
const testDirectoryWithBlockedFirstPortionFixture = getFixture({
    type: 'public_list',
    params: { hash: '/+dWODt1e4HskzD1QFVevBqJzbZ7bbHr4F3iI95rlqk=111' },
    formatted: true
});

describe('resorces actions', () => {
    let store;
    beforeEach(() => {
        resetListPortionsPromises();
        store = getStore({});
        store.dispatch(setUrl({ host: 'yadi.sk', pathname: '/d/3wMPH8ob3KKMBa', query: {} }));
        store.dispatch(updateResources(testDirectoryFixture.resources));
        store.dispatch(updateResources(testDirectoryFirstPortionFixture.resources));
        store.dispatch(updateResources(testDirectoryWithBlockedFirstPortionFixture.resources));
        store.dispatch(setCurrentResource(testDirectoryId));
        store.clearActions();
    });

    describe('setCurrentResource', () => {
        it('переход в незагруженную папку', () => {
            const resource = store.getState().resources[testSubDirectoryId];
            expect(resource.children.length).toBe(0);
            expect(resource.isFirstPortionLoaded).toBe(false);
            return store.dispatch(setCurrentResource(testSubDirectoryId)).then(() => {
                const resource = store.getState().resources[testSubDirectoryId];
                expect(resource.children.length).toBe(40);
                expect(resource.isFirstPortionLoaded).toBe(true);
                expect(store.getActions()).toMatchSnapshot();
            });
        });

        it('переход в загруженную папку', () => {
            store.dispatch(updateResources(testSubDirectoryFirstPortion.resources));
            const resource = store.getState().resources[testSubDirectoryId];
            expect(resource.children.length).toBe(40);
            expect(resource.isFirstPortionLoaded).toBe(true);
            return store.dispatch(setCurrentResource(testSubDirectoryId)).then(() => {
                expect(store.getActions()).toMatchSnapshot();
            });
        });

        it('переход в загруженную папку с протухшими превью', () => {
            store.dispatch(updateResources(testSubDirectoryFirstPortion.resources));
            const resource = store.getState().resources[testSubDirectoryId];
            expect(resource.children.length).toBe(40);
            expect(resource.isFirstPortionLoaded).toBe(true);
            const originalDate = Date.now;
            Date.now = () => 1537190300000 + 4 * 3600 * 1000;
            return store.dispatch(setCurrentResource(testSubDirectoryId)).then(() => {
                expect(store.getActions()).toMatchSnapshot();
                Date.now = originalDate;
            });
        });

        it('при переходе в подпапку должно сниматься выделение', () => {
            store.dispatch(setSelectedResources([testSubDirectoryId]));
            store.dispatch(setCurrentResource(testSubDirectoryId));
            expect(store.getState().selectedResources).toEqual([]);
        });
    });

    describe('fetchNextPortion', () => {
        it('первая порция', () => {
            const resource = store.getState().resources[testDirectoryId];
            expect(resource.children.length).toBe(40);
            expect(resource.countBlockedItems).toBe(0);
            expect(resource.completed).toBe(false);
        });

        it('вторая порция', () => {
            return store.dispatch(fetchNextPortion(testDirectoryId))
                .then(() => {
                    const resource = store.getState().resources[testDirectoryId];
                    expect(resource.children.length).toBe(80);
                    expect(resource.countBlockedItems).toBe(0);
                    expect(resource.completed).toBe(false);
                    expect(store.getState()).toMatchSnapshot();
                    expect(store.getActions()).toMatchSnapshot();
                });
        });

        it('последняя порция', () => {
            return store.dispatch(fetchNextPortion(testDirectoryId))
                .then(() => store.dispatch(fetchNextPortion(testDirectoryId)))
                .then(() => {
                    const resource = store.getState().resources[testDirectoryId];
                    expect(resource.children.length).toBe(85);
                    expect(resource.countBlockedItems).toBe(0);
                    expect(resource.completed).toBe(true);
                    expect(store.getState()).toMatchSnapshot();
                    expect(store.getActions()).toMatchSnapshot();
                });
        });

        it('первая порция, когда в ней есть заблокированные ресурсы', () => {
            const resource = store.getState().resources[testDirectoryWithBlockedId];
            expect(resource.children.length).toBe(1);
            expect(resource.countBlockedItems).toBe(39);
            expect(resource.completed).toBe(false);
        });

        it('вторая порция, когда в первой есть заблокированные ресурсы', () => {
            return store.dispatch(fetchNextPortion(testDirectoryWithBlockedId))
                .then(() => {
                    const resource = store.getState().resources[testDirectoryWithBlockedId];
                    expect(resource.children.length).toBe(2);
                    expect(resource.countBlockedItems).toBe(39);
                    expect(resource.completed).toBe(true);
                    expect(store.getState()).toMatchSnapshot();
                    expect(store.getActions()).toMatchSnapshot();
                });
        });
    });

    describe('checkLoadListingForResource', () => {
        it('искомый ресурс в первой порции', () => {
            return store.dispatch(
                checkLoadListingForResource('089985f114eb38fe4ff07ac6300ad4e033fb273450cfc9b4d1004b076e54e076')
            ).then(() => {
                expect(store.getActions()).toMatchSnapshot();
            });
        });

        it('искомый ресурс во второй порции', () => {
            return store.dispatch(
                checkLoadListingForResource('28a8ba0a54ef624f97abdab39ccd281d79e606fec9614773e6b6139f3fae11f7')
            ).then(() => {
                expect(store.getActions()).toMatchSnapshot();
            });
        });

        it('искомый ресурс во третьей порции', () => {
            return store.dispatch(
                checkLoadListingForResource('0b5455772c4b0c78cd34d8c428eec847dc3794a8aa7fd7e912b1ac0f9f07086b')
            ).then(() => {
                expect(store.getActions()).toMatchSnapshot();
            });
        });

        it('ресурса нет ни во одной порции', () => {
            return store.dispatch(
                checkLoadListingForResource('10b5455772c4b0c78cd34d8c428eec847dc3794a8aa7fd7e912b1ac0f9f07086b')
            ).catch((error) => {
                expect(error).toBeUndefined();
                expect(store.getActions()).toMatchSnapshot();
            });
        });
    });

    describe('checkLoadListingForSlider', () => {
        it('слайдер должен закрываться если ресурса не нашлось', () => {
            store.dispatch(_openSlider('10b5455772c4b0c78cd34d8c428eec847dc3794a8aa7fd7e912b1ac0f9f07086b'));
            store.clearActions();

            return store.dispatch(checkLoadListingForSlider()).then(() => {
                expect(store.getActions()).toMatchSnapshot();
            });
        });
    });

    describe('fetchDirSize', () => {
        it('должен обновлять meta.size', () => {
            return store.dispatch(fetchDirSize(testDirectoryFirstPortionFixture.resources[1].id))
                .then(() => {
                    const resource = store.getState().resources[testDirectoryFirstPortionFixture.resources[1].id];
                    expect(resource.meta.size).toBe(62748244);
                    expect(resource.meta.files_count).toBe(45);
                    expect(store.getState()).toMatchSnapshot();
                });
        });
    });
});
