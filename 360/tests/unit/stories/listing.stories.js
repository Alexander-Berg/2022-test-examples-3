import '../noscript';

import { storiesOf, specs, describe, it, mount } from '../.storybook/facade';
import React from 'react';
import getStore from '../../../components/redux/store';
import { updatePage } from '../../../components/redux/store/actions';
import { updateEnvironment } from '../../../components/redux/store/actions/environment';
import { setResourceData } from '../../../components/redux/store/actions/resources';
import { Provider } from 'react-redux';

import Listing from '../../../components/redux/components/listing/listing';

import fixtures from '../../fixtures/resources';

import * as rawFetchModel from '../../../components/redux/store/lib/raw-fetch-model';

jest.mock('@ps-int/ufo-rocks/lib/helpers/pointer', () => () => ({ clearListeners: () => {} }));
jest.mock('@ps-int/ufo-rocks/lib/components/clamped-text', () => ({ text }) => <span>{text}</span>);

jest.mock('../../../components/helpers/performance', () => ({
    withResourceTimingsPreserved: (promise) => promise.then((data) => Promise.resolve([data, []])),
    collectTimings: () => {},
    sendHeroElement: () => {}
}));

jest.mock('../../../components/helpers/page', () => ({
    go: () => Promise.resolve({}),
    setOnBeforeFirstRenderCallback: () => {}
}));

import resourceHelper from '../../../components/helpers/resource';
const diskId = '/disk';
getStore().dispatch(
    setResourceData(diskId, Object.assign({ id: diskId }, resourceHelper.DEFAULT_FOLDERS_DATA[diskId]))
);

const getComponent = (store) => (
    <Provider store={store}>
        <Listing/>
    </Provider>
);

const getTestStore = () => {
    const store = getStore();
    store.dispatch(updatePage({
        idContext: diskId,
        originalNSParams: {
            idContext: diskId,
            idApp: 'client'
        }
    }));

    store.dispatch(updateEnvironment({
        session: { experiment: {} }, agent: {}
    }));
    return store;
};

export default storiesOf('Listing', module)
    .add('re-renders', ({ kind }) => {
        specs(() => describe(kind, () => {
            const originalRawFetchModel = rawFetchModel.default;
            const originalGetEntriesByType = global.performance.getEntriesByType;
            const resourceId = fixtures.listingResources[2].id;
            beforeAll(() => {
                rawFetchModel.default = jest.fn((modelName) => {
                    let data = {};
                    if (modelName === 'resources') {
                        data = {
                            resources: [...fixtures.listingResources]
                        };
                    }
                    return Promise.resolve(data);
                });

                global.performance.getEntriesByType = () => ([{
                    initiatorType: 'xmlhttprequest',
                    name: location.origin + ns.request.URL + '?_m=resources',
                    duration: 0
                }]);

                ns.page.current.params = {};
            });
            afterAll(() => {
                rawFetchModel.default = originalRawFetchModel;
                global.performance.getEntriesByType = originalGetEntriesByType;
            });

            const store = getTestStore();
            const component = getComponent(store);

            it('should not re-render if none of listing resources changed (but current resource changed)', (done) => {
                const wrapper = mount(component);
                // данные синкаются из моделей в `store` асинхронно
                // ToDo: выпилить таймауты когда избавимся от ns (ну или хотя бы сделаем `store` первостепенным)
                setTimeout(() => {
                    // проверяем что ресурсы загрузились
                    expect(store.getState().resources[diskId].isLoading).toEqual(false);
                    expect(store.getState().resources[resourceId]).not.toBeUndefined();

                    const listingItemsComponent = wrapper.find('ListingItems').instance();
                    const listingItemsRenderSpy = jest.spyOn(listingItemsComponent, 'render');

                    // пометим корневой ресурс как загружающийся
                    getStore().dispatch(setResourceData(diskId, true, 'isLoading'));

                    // данные синкаются из моделей в `store` асинхронно
                    setTimeout(() => {
                        // проверим что корневой ресурс пометился как загружающийся
                        expect(store.getState().resources[diskId].isLoading).toEqual(true);

                        // проверим что не было ре-рендеринга списка ресурсов в листинге
                        expect(listingItemsRenderSpy).toHaveBeenCalledTimes(0);

                        wrapper.unmount();
                        done();
                    }, 200);
                }, 500);
            });

            it('should re-render if any of listing resources changed', (done) => {
                const wrapper = mount(component);
                // данные синкаются из моделей в `store` асинхронно
                // ToDo: выпилить таймауты когда избавимся от ns (ну или хотя бы сделаем `store` первостепенным)
                setTimeout(() => {
                    // проверим что ресурс непубличный
                    expect(store.getState().resources[resourceId].meta.public).toBeUndefined();

                    const listingItemsComponent = wrapper.find('ListingItems').instance();
                    const listingItemsRenderSpy = jest.spyOn(listingItemsComponent, 'render');

                    // делаем ресурс публичным
                    store.dispatch(setResourceData(resourceId, 1, 'meta.public'));

                    // данные синкаются из моделей в `store` асинхронно
                    setTimeout(() => {
                        // проверим что ресурс стал публичным
                        expect(store.getState().resources[resourceId].meta.public).toEqual(1);

                        // проверим что был ре-рендеринг списка ресурсов в листинге
                        expect(listingItemsRenderSpy).toHaveBeenCalledTimes(1);

                        wrapper.unmount();
                        done();
                    }, 200);
                }, 200);
            });
        }));
    });
