import React from 'react';
import B2cBanner from '../../../components/redux/components/b2c-1-banner';
import { B2cBanner as B2cBannerWithoutProvider } from '../../../components/redux/components/b2c-1-banner';
import { mount } from 'enzyme';
import { B2C_BANNER_DURATION_SHOW_TIMEOUT } from '../../../components/consts';
import { Provider } from 'react-redux';

import { createStore } from 'redux';
import reducer from '../../../components/redux/store/reducers/promo';
import { count } from '../../../components/helpers/metrika';
import { saveSettings } from '../../../components/redux/store/actions/settings';

jest.mock('../../../components/redux/store/actions/settings', () => ({
    saveSettings: jest.fn(() => ({ type: 'MOCK' }))
}));

jest.mock('../../../components/helpers/metrika', () => ({
    count: jest.fn()
}));

jest.mock('../../../components/redux/store', () => ({
    getStore: jest.fn(() => {})
}));

Date.now = jest.fn(() => B2C_BANNER_DURATION_SHOW_TIMEOUT);

const getStore = (state = {}) => {
    return createStore(reducer, Object.assign({
        settings: {
            timestampLastB2CBannerShow: '0',
            isNeedToResetB2cBannerShowDuration: '0'
        },
        environment: {
            session: {
                serverTime: Date.now()
            }
        },
        page: {
            originalNSParams: {
                server_time: undefined
            }
        }
    }, state));
};

describe('B2cBanner', () => {
    it('Показываем баннер, если длительность показа удовлетворительная. Но при этом "помечаем"' +
        ' дату первого показа баннера, если еще не', () => {
        const store = getStore({
            settings: {
                timestampLastB2CBannerShow: String(Date.parse(new Date(2021, 4, 26, 23, 59, 59)))
            },
            environment: {
                session: {
                    serverTime: Date.parse(new Date(2021, 4, 27, 23, 59, 59))
                }
            }
        });

        const component = mount(<Provider store={store}><B2cBanner /></Provider>);
        expect(saveSettings.mock.calls[0][0]).toEqual(
            { key: 'isNeedToResetB2cBannerShowDuration', value: '1', serverOnly: true });
        expect(count).toHaveBeenCalledWith(
            'interface elements', 'Disk', 'web_client', 'Top_banner', 'B2B_subscription', 'show');
        expect(saveSettings.mock.calls[1][0]).toEqual(
            { key: 'timestampLastB2CBannerShow', value: String(Date.now()), serverOnly: true });
        expect(component).toMatchSnapshot();
    });

    it('Если в баннере прошла длительность в N дней, надо закрыть его за юзера и сбросить настройку показа', () => {
        const props = {
            timestampLastB2CBannerShow: String(Date.parse(new Date(2021, 4, 26, 23, 59, 59))),
            serverTime: new Date(2021, 4, 31, 23, 59, 59),
            isNeedToResetB2cBannerShowDuration: '1',
            saveSettings: jest.fn(() => Promise.resolve({}))
        };

        const onClose = jest.spyOn(B2cBannerWithoutProvider.prototype, '_onClose');
        const ref = React.createRef();

        mount(<Provider store={getStore({})}><B2cBannerWithoutProvider ref={ref} {...props}/></Provider>);

        expect(props.saveSettings.mock.calls[0][0]).toEqual(
            { key: 'isNeedToResetB2cBannerShowDuration', value: '0', serverOnly: true });
        expect(onClose).toHaveBeenCalledTimes(1);
        expect(props.saveSettings.mock.calls[1][0]).toEqual(
            { key: 'timestampLastClosedB2CBanner', value: String(Date.now()), serverOnly: true });
        expect(props.saveSettings.mock.calls[2][0]).toEqual(
            { key: 'timestampLastB2CBannerShow', value: '0', serverOnly: true });
        jest.restoreAllMocks();
    });

    it('Если можно снова показывать баннер через N дней смотрим на длительность показа и если ок то показываем', () => {
        const store = getStore({
            settings: {
                timestampLastB2CBannerShow: '0',
                isNeedToResetB2cBannerShowDuration: '0'
            },
            environment: {
                session: {
                    serverTime: new Date(2021, 5, 1, 23, 59, 59)
                }
            }
        });

        const ref = React.createRef();
        ref.current = B2cBannerWithoutProvider;
        const metrikaCount = jest.spyOn(B2cBannerWithoutProvider.prototype, '_metrikaCount');

        mount(<Provider store={store}><B2cBanner ref={ref}/></Provider>);

        expect(saveSettings.mock.calls[0][0]).toEqual(
            { key: 'isNeedToResetB2cBannerShowDuration', value: '1', serverOnly: true });
        expect(saveSettings.mock.calls[1][0]).toEqual(
            { key: 'timestampLastB2CBannerShow', value: String(Date.now()), serverOnly: true });
        expect(metrikaCount).toHaveBeenCalledWith('show');
    });

    it('Если передано серверное время через гет-параметр, то в настройках выставляем именно его', () => {
        const store = getStore({
            settings: {
                timestampLastB2CBannerShow: String(Date.parse(new Date(2021, 4, 26, 23, 59, 59)))
            },
            environment: {
                session: {
                    serverTime: Date.now()
                }
            },
            page: {
                originalNSParams: {
                    server_time: String(Date.parse(new Date(2021, 4, 27, 23, 59, 59)))
                }
            }
        });

        mount(<Provider store={store}><B2cBanner /></Provider>);
        expect(saveSettings.mock.calls[0][0]).toEqual(
            { key: 'isNeedToResetB2cBannerShowDuration', value: '1', serverOnly: true });
        expect(count).toHaveBeenCalledWith(
            'interface elements', 'Disk', 'web_client', 'Top_banner', 'B2B_subscription', 'show');
        expect(saveSettings.mock.calls[1][0]).toEqual(
            { key: 'timestampLastB2CBannerShow', value: String(Date.now()), serverOnly: true });
    });

    it('Если можно снова показывать баннер через N дней и выставляли флажок "закрепления" первой даты показа,' +
        ' то просто сыпем метрику показа', () => {
        jest.resetAllMocks();

        const store = getStore({
            settings: {
                timestampLastB2CBannerShow: String(Date.parse(new Date(2021, 4, 26, 23, 59, 59))),
                isNeedToResetB2cBannerShowDuration: '1'
            },
            environment: {
                session: {
                    serverTime: Date.parse(new Date(2021, 4, 27, 23, 59, 59))
                }
            }
        });

        const ref = React.createRef();
        ref.current = B2cBannerWithoutProvider;
        const metrikaCount = jest.spyOn(B2cBannerWithoutProvider.prototype, '_metrikaCount');

        mount(<Provider store={store}><B2cBanner ref={ref}/></Provider>);

        expect(saveSettings).toHaveBeenCalledTimes(0);
        expect(metrikaCount).toHaveBeenCalledWith('show');
    });

    it('Если юзер сам закрыл баннер руками, то через 3 дня показываем снова 2 дня, после этого снова не показываем, ' +
        'закрываем за него', () => {
        const props = {
            timestampLastB2CBannerShow: String(Date.parse(new Date(2021, 5, 1, 17, 39, 55))),
            serverTime: Date.parse(new Date(2021, 5, 1, 17, 39, 55)),
            isNeedToResetB2cBannerShowDuration: '1',
            saveSettings: jest.fn(() => Promise.resolve({})),
            hasServerTimeFromGetParameter: true
        };

        const wrapper = mount(<B2cBannerWithoutProvider ref={null} {...props} />);
        jest.spyOn(wrapper.instance(), '_onClose');

        wrapper.instance()._onClose();
        expect(props.saveSettings).toHaveBeenCalledTimes(3);
        expect(props.saveSettings.mock.calls[0][0]).toEqual({
            key: 'isNeedToResetB2cBannerShowDuration',
            value: '0',
            serverOnly: true
        });
        expect(props.saveSettings.mock.calls[1][0]).toEqual({
            key: 'timestampLastClosedB2CBanner',
            value: String(Date.parse(new Date(2021, 5, 1, 17, 39, 55))),
            serverOnly: true
        });
        expect(props.saveSettings.mock.calls[2][0]).toEqual({
            key: 'timestampLastB2CBannerShow',
            value: '0',
            serverOnly: true
        });
        jest.restoreAllMocks();
    });
});
