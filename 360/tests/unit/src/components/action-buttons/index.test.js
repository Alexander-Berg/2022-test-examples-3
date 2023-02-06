import ActionButtons from '../../../../../src/components/action-buttons';

import React from 'react';
import { render, mount } from 'enzyme';
import getStore from '../../store';

import { Provider } from 'react-redux';
jest.mock('../../../../../src/lib/metrika');
jest.mock('../../../../../src/store/async-actions');

const runTest = (state, props) => {
    const store = getStore(Object.assign({
        ua: {},
        url: {
            query: {}
        },
        overlays: {
            activePane: {}
        },
        environment: {
            experiments: { flags: {} }
        }
    }, state));
    const component = render(
        <Provider store={store}>
            <ActionButtons {...props}/>
        </Provider>
    );
    expect(component).toMatchSnapshot();
};

const rootResourceId = 'rootResourceId1';

describe('action-buttons =>', () => {
    it('дефолтное состояние для файла', () => {
        runTest({
            rootResourceId,
            resources: {
                [rootResourceId]: {
                    meta: {}
                }
            }
        });
    });

    it('дефолтное состояние для папки', () => {
        runTest({
            rootResourceId,
            resources: {
                [rootResourceId]: {
                    type: 'dir',
                    meta: {}
                }
            }
        });
    });

    it('с иконками', () => {
        runTest({
            rootResourceId,
            resources: {
                [rootResourceId]: {
                    meta: {}
                }
            }
        }, {
            hasIcons: true
        });
    });

    it('скрытие при опции `hideOnSliderOrPaneOpen: true` и открытой панели', () => {
        runTest({
            rootResourceId,
            resources: {
                [rootResourceId]: {
                    meta: {}
                }
            },
            overlays: {
                activePane: {
                    type: 'info'
                }
            }
        }, {
            hideOnSliderOrPaneOpen: true
        });
    });

    it('скрытие при опции `hideOnSliderOrPaneOpen: true` и открытом слайдере', () => {
        runTest({
            rootResourceId,
            resources: {
                [rootResourceId]: {
                    meta: {}
                }
            },
            overlays: {
                activePane: {},
                sliderResourceId: rootResourceId
            }
        }, {
            hideOnSliderOrPaneOpen: true
        });
    });
});

describe('action-buttons (APP) =>', () => {
    beforeEach(() => {
        global.APP = true;
    });
    afterEach(() => {
        global.APP = false;
    });

    it('в приложении нет кнопки "Скачать"', () => {
        runTest({
            rootResourceId,
            resources: {
                [rootResourceId]: {
                    meta: {}
                }
            }
        });
    });

    it('не должен скрываться при опции `hideOnSliderOrPaneOpen: true` и открытом слайдере', () => {
        runTest({
            rootResourceId,
            resources: {
                [rootResourceId]: {
                    meta: {}
                }
            },
            overlays: {
                activePane: {
                    sliderResourceId: rootResourceId
                }
            }
        }, {
            hideOnSliderOrPaneOpen: true
        });
    });
});

describe('action-buttons для антиФО =>', () => {
    const getState = (isSmartphone, rootResourceType = 'file') => getStore({
        rootResourceId,
        resources: {
            [rootResourceId]: {
                meta: {},
                type: rootResourceType
            }
        },
        ua: {
            isSmartphone
        },
        url: {
            query: {}
        },
        overlays: {
            activePane: {}
        },
        services: {
            passport: ''
        },
        environment: {
            antiFileSharing: true,
            experiments: { flags: {} }
        }
    });

    it('на десктопах должно быть промо подписки на почту 360', () => {
        const component = mount(
            <Provider store={getState(false)}>
                <ActionButtons />
            </Provider>
        );
        expect(component.render()).toMatchSnapshot();
        component.unmount();
    });

    it('на десктопах у папки нет кнопки Скачать и показан тултип', () => {
        const component = mount(
            <Provider store={getState(false, 'dir')}>
                <ActionButtons />
            </Provider>
        );
        expect(component.render()).toMatchSnapshot();
        component.unmount();
    });

    it('на мобилах должно быть промо подписки на почту 360', () => {
        const component = mount(
            <Provider store={getState(true)}>
                <ActionButtons fromBottomToolbar />
            </Provider>
        );
        expect(component.render()).toMatchSnapshot();
        component.unmount();
    });
});
