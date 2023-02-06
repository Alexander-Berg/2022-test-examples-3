import Notification from '../../../../../../src/components/bottom-toolbar/notification';
import { NOTIFICATION_TYPES } from '../../../../../../src/store/constants';

import React from 'react';
import { render } from 'enzyme';
import getStore from '../../../store';

import { Provider } from 'react-redux';

const tariff = {
    display_space: 100,
    currency: 'RUB',
    periods: {
        year: {
            price: 800,
            product_id: '100gb_1y_2018'
        },
        month: {
            price: 80,
            product_id: '100gb_1m_2018'
        }
    },
    display_space_units: 'ГБ',
    space: 107374182400
};

const resourceWithPreviewId = 'resourceWithPreviewId';
const resourceWithoutPreviewId = 'resourceWithoutPreviewId';
const runTest = (notification) => {
    const store = getStore({
        resources: {
            [resourceWithPreviewId]: {
                name: 'resource-with-preview.jpg',
                type: 'file',
                meta: {
                    defaultPreview: 'https://preview-base-url?param=value'
                }
            },
            [resourceWithoutPreviewId]: {
                name: 'folder-name',
                type: 'dir',
                meta: {}
            }
        }
    });
    const component = render(
        <Provider store={store}>
            <Notification notification={notification}/>
        </Provider>
    );
    expect(component).toMatchSnapshot();
};

describe('bottom-toolbar/notification =>', () => {
    it('дефолтное состояние (без привязки к ресурсу)', () => {
        runTest({
            text: 'default notification text'
        });
    });

    it('нотифайка с привязкой к ресурсу', () => {
        runTest({
            resourceId: resourceWithPreviewId,
            text: 'notification without resource text'
        });
    });

    it('нотифайка об успешном сохранении (ресурс с превью)', () => {
        runTest({
            resourceId: resourceWithPreviewId,
            type: NOTIFICATION_TYPES.SAVE_TO_DISK_SUCCESS
        });
    });

    it('нотифайка об успешном сохранении (ресурс без превью)', () => {
        runTest({
            resourceId: resourceWithoutPreviewId,
            type: NOTIFICATION_TYPES.SAVE_TO_DISK_SUCCESS
        });
    });

    it('нотифайка об ошибке сохранения (ресурс с превью)', () => {
        runTest({
            resourceId: resourceWithPreviewId,
            type: NOTIFICATION_TYPES.SAVE_TO_DISK_ERROR
        });
    });

    it('нотифайка об ошибке сохранения (ресурс без превью)', () => {
        runTest({
            resourceId: resourceWithoutPreviewId,
            type: NOTIFICATION_TYPES.SAVE_TO_DISK_ERROR
        });
    });

    it('нотифайка о недостатке места для сохранения (ресурс с превью)', () => {
        runTest({
            resourceId: resourceWithPreviewId,
            type: NOTIFICATION_TYPES.SAVE_TO_DISK_ERROR_SPACE,
            extraData: {
                tariff
            }
        });
    });

    it('нотифайка о недостатке места для сохранения (ресурс без превью)', () => {
        runTest({
            resourceId: resourceWithoutPreviewId,
            type: NOTIFICATION_TYPES.SAVE_TO_DISK_ERROR_SPACE,
            extraData: {
                tariff
            }
        });
    });
});
