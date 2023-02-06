import * as React from 'react';
import { mount } from 'enzyme';
import * as serializer from 'jest-serializer-html';
import { LcLazyImageComponent } from '../LcLazyImage';

expect.addSnapshotSerializer(serializer);

describe('LcLazyImage component', () => {
    test('should pass visibility prop and ref', () => {
        const visibilityRootRef = React.createRef<HTMLImageElement>();
        const forwardedRef = React.createRef<LcLazyImageComponent>();
        const unobserve = jest.fn();
        const image = {
            url: 'https://avatars.mds.yandex.net/get-lpc/foo/bar/orig',
            width: 100,
            height: 100,
        };
        const component = mount(
            // @ts-ignore
            <LcLazyImageComponent
                isVisible
                isIntersecting
                visibilityRootRef={visibilityRootRef}
                forwardedRef={forwardedRef}
                unobserve={unobserve}
                value={image}
            />
        );

        const lcPicture = component.find('LcPictureComponent');

        expect(lcPicture.props()).toMatchSnapshot();
    });
});
