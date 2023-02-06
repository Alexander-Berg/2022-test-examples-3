import AudioPlayer from '../../../../../src/components/audio-player';

import React from 'react';
import { mount } from 'enzyme';
import getStore from '../../store';

import { Provider } from 'react-redux';

jest.mock('../../../../../src/store/async-actions');
import { getDownloadUrl } from '../../../../../src/store/async-actions';

const audioResourceId = 'audio-resource-id';

it('audio-player', () => {
    const store = getStore({
        resources: {
            [audioResourceId]: {
                name: 'audio-resource.mp3',
                meta: {
                    mimetype: 'audio/mp3'
                }
            }
        },
        ua: {},
        environment: {}
    });
    const countMetrikaFn = jest.fn();
    const component = mount(
        <Provider store={store}>
            <AudioPlayer resourceId={audioResourceId} countMetrika={countMetrikaFn}/>
        </Provider>
    );
    expect(component.render()).toMatchSnapshot();

    const countMetrikaCalls = popFnCalls(countMetrikaFn);
    expect(countMetrikaCalls.length).toEqual(1);
    expect(countMetrikaCalls[0]).toEqual(['show']);

    const getDownloadUrlCalls = popFnCalls(getDownloadUrl);
    expect(getDownloadUrlCalls.length).toEqual(1);
    expect(getDownloadUrlCalls[0]).toEqual([audioResourceId]);

    component.unmount();
});
