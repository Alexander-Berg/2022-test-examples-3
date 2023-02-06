import BottomToolbar from '../../../../../src/components/bottom-toolbar';
import { NOTIFICATIONS_STATES, NOTIFICATION_MODES } from '../../../../../src/store/constants';

import React from 'react';
import { render } from 'enzyme';
import getStore from '../../store';

import { Provider } from 'react-redux';

const runTest = (state) => {
    const store = getStore(Object.assign({
        ua: {},
        url: {
            query: {}
        },
        environment: {
            experiments: { flags: {} }
        }
    }, state));
    const component = render(
        <Provider store={store}>
            <BottomToolbar/>
        </Provider>
    );
    expect(component).toMatchSnapshot();
};

const rootResourceId = 'rootResourceId1';

describe('bottom-toolbar =>', () => {
    it('дефолтное состояние', () => {
        runTest({
            rootResourceId,
            resources: {
                [rootResourceId]: {
                    meta: {}
                }
            }
        });
    });

    it('с нотифайкой TEXT', () => {
        runTest({
            rootResourceId,
            resources: {
                [rootResourceId]: {
                    meta: {}
                }
            },
            notifications: {
                state: NOTIFICATIONS_STATES.OPENED,
                current: {
                    mode: NOTIFICATION_MODES.TEXT,
                    text: 'notification text'
                }
            }
        });
    });

    it('с нотифайкой ERROR_TEXT', () => {
        runTest({
            rootResourceId,
            resources: {
                [rootResourceId]: {
                    meta: {}
                }
            },
            notifications: {
                state: NOTIFICATIONS_STATES.OPENING,
                current: {
                    mode: NOTIFICATION_MODES.ERROR_TEXT,
                    text: 'error notification text'
                }
            }
        });
    });

    it('с нотифайкой TEXT_BUTTON', () => {
        runTest({
            rootResourceId,
            resources: {
                [rootResourceId]: {
                    meta: {}
                }
            },
            notifications: {
                state: NOTIFICATIONS_STATES.OPENED,
                current: {
                    mode: NOTIFICATION_MODES.TEXT_BUTTON,
                    text: 'notification with button text',
                    buttonText: 'text on button'
                }
            }
        });
    });
});
