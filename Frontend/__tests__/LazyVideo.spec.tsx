import * as React from 'react';
import { shallow } from 'enzyme';
import * as serializer from 'jest-serializer-html';

import { LazyVideoComponent } from '../LazyVideo';

expect.addSnapshotSerializer(serializer);

describe('LazyVideo component', () => {
    test('should pass visibility props and video props', () => {
        const visibilityRootRef = React.createRef<HTMLVideoElement>();
        const unobserve = jest.fn();
        const props = {
            className: 'video-object',
            autoPlay: true,
            playsInline: true,
            loop: true,
            muted: true,
            controls: true,
            preload: 'auto',
            mp4Url: 'https://streaming.video.yandex.ru/get/yndx-video/m-69033-1690643b9c3-c491e9a08b6caf39/480p.mp4',
            webmUrl: 'https://streaming.video.yandex.ru/get/yndx-video/m-69033-1690643b9c3-c491e9a08b6caf39/480p.webm',
        };
        const component = shallow(
            <LazyVideoComponent
                isVisible
                isIntersecting
                visibilityRootRef={visibilityRootRef}
                unobserve={unobserve}

                {...props}
            />
        );

        expect(component.html()).toMatchSnapshot();
    });
});
