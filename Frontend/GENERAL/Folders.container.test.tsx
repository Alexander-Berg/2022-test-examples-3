import { mount, ReactWrapper } from 'enzyme';
import React, { ReactNode } from 'react';
import { Provider } from 'react-redux';
import configureStore, { MockStore } from 'redux-mock-store';

import { mockStoreData } from '../../test-data/storeData';
import { mockFolders } from './Folders.mock';

import { FoldersConnected } from './Folders.container';
import { ProxyContext } from '~/src/features/Dispenser';

jest.mock('~/src/common/hooks/useNotification');

const withContext = (Component: ReactNode) => (
    <ProxyContext.Provider
        // @ts-ignore тут не нужен полный шейп контекста
        value={{
            slug: 'd',
            configs: {
                hosts: {
                    datalens: {
                        protocol: 'https:',
                        hostname: 'datalens.yandex-team.ru',
                    },
                    wiki: {
                        protocol: 'https:',
                        hostname: 'wiki.yandex-team.ru',
                    },
                    forms: {
                        protocol: 'https:',
                        hostname: 'forms.yandex-team.ru',
                    },
                },
            },
        }}
    >
        {Component}
    </ProxyContext.Provider>
);

const mockStore = configureStore();

const serviceId = 3325;
const notLoadedServiceId = 3333;

let store: MockStore;
let component: ReactWrapper;

describe('Dispenser Folders', () => {
    beforeEach(() => {
        store = mockStore({
            ...mockStoreData,
            folders: {
                [serviceId]: {
                    ...mockFolders,
                },
            },
        });
    });

    afterEach((): void => {
        component.unmount();
    });

    it('should perform fetch trigger action with service id', () => {
        const actions = store.getActions();

        component = mount(withContext(
            <Provider store={store}>
                <FoldersConnected
                    serviceId={notLoadedServiceId}
                />
            </Provider>,
        ));

        expect(actions.length).toEqual(1);
        expect(actions[0].payload).toEqual({ serviceId: notLoadedServiceId });
    });

    it('should perform "PROVIDERS_FETCH" trigger action when folders presented', () => {
        const actions = store.getActions();

        component = mount(withContext(
            <Provider store={store}>
                <FoldersConnected
                    serviceId={serviceId}
                />
            </Provider>,
        ));

        expect(actions.length).toEqual(1);
        expect(actions[0].type).toEqual('dispenser/Hardware/Folders/PROVIDERS_FETCH/TRIGGER');
    });
});
