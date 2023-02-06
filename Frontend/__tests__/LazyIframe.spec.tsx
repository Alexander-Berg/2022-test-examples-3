import * as React from 'react';
import { shallow } from 'enzyme';
import * as serializer from 'jest-serializer-html';

import { LazyIframeComponent } from '../LazyIframe';
import { ILazyIframeProps } from '../LazyIframe.types';

expect.addSnapshotSerializer(serializer);

describe('LazyIframe component', () => {
    test('should pass visibility props and iframe props', () => {
        const visibilityRootRef = React.createRef<HTMLIFrameElement>();
        const unobserve = jest.fn();
        const props: ILazyIframeProps = {
            className: 'video-object',
            src: 'https://www.youtube.com/embed/jiOe-Ar8KOk',
            frameBorder: 'no',
            scrolling: 'no',
            allowFullScreen: true,
        };
        const component = shallow(
            <LazyIframeComponent
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
