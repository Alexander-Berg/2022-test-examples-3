import React from 'react';
import { mount } from 'enzyme';
import getStore from '../../store';

import { Provider } from 'react-redux';

import { countMainBranch } from '../../../../../src/lib/metrika';

/* ErrorComponent различается для web и приложения (в момент подтягивания зависимости по-разному require-ится Direct) */
export const runTest = (ErrorComponent, { errorCode, blocked, noAdv }) => {
    const store = getStore({
        url: {
            query: {}
        },
        rootResourceId: 'error',
        resources: {
            error: {
                errorCode,
                blocked
            }
        },
        services: {
            legal: 'https://yandex.ru/legal',
            disk: 'https://disk.yandex.ru'
        },
        environment: {
            noAdv,
            experiments: {
                flags: {}
            },
            nonce: 'nonce'
        }
    });
    expect(popFnCalls(countMainBranch).length).toEqual(0);
    const component = mount(
        <Provider store={store}>
            <ErrorComponent/>
        </Provider>
    );
    expect(component.render()).toMatchSnapshot();
    expect(component.find('.direct-public_position_bottom').length).toEqual(noAdv || APP ? 0 : 1);

    const countMainBranchCalls = popFnCalls(countMainBranch);
    expect(countMainBranchCalls.length).toEqual(1);
    const code = blocked ? 'blocked' :
        (errorCode === 404 ? 'removed' : errorCode);
    expect(countMainBranchCalls[0]).toEqual(['error', code]);
    component.unmount();
};
