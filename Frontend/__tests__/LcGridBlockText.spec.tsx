import * as React from 'react';
import { mount, ReactWrapper } from 'enzyme';
import { BackgroundType, BackgroundPosition } from '@yandex-turbo/components/LcBackground/LcBackground.types';
import { LcEventType, LcSectionType } from '@yandex-turbo/components/LcEvents/LcEvents.constants';
import { UrlTypes } from '@yandex-turbo/components/LcLink/LcLink.types';
import { Unit, LcAlign, LcVerticalAlign } from '@yandex-turbo/components/lcTypes/lcTypes';
import { LcGridBlockText } from '../LcGridBlockText';
import { ILcGridBlockTextProps } from '../LcGridBlockText.types';

describe('<LcGridBlockText/> component', () => {
    let wrapper: ReactWrapper;

    beforeEach(() => {
        // @ts-ignore
        global.MutationObserver = class {
            constructor() {}
            disconnect() {}
            observe() {}
        };
    });

    afterEach(() => {
        wrapper.unmount();

        // @ts-ignore
        delete global.MutationObserver;
    });

    test('should pass events to link', () => {
        const props: ILcGridBlockTextProps = {
            paddings: {
                top: { value: 0, unit: Unit.Pixel },
                right: { value: 0, unit: Unit.Pixel },
                bottom: { value: 0, unit: Unit.Pixel },
                left: { value: 0, unit: Unit.Pixel },
            },
            align: {
                horizontal: LcAlign.CENTER,
                vertical: LcVerticalAlign.Center,
            },
            background: {
                type: BackgroundType.Image,
                positions: BackgroundPosition.CenterCenter,
                baseUrl: 'https://avatars.mds.yandex.net/get-lpc/1370085/be1172f0-5642-4874-9a44-2b6afd6d412d/orig',
            },
            link: {
                url: 'https://yandex.ru/',
                text: '',
                type: UrlTypes.Url,
                openInNewTab: true,
            },
            mobileProps: {
                grid: {
                    x: 2,
                    y: 1,
                    width: 2,
                    height: 1,
                },
                paddings: {
                    top: { value: 0, unit: Unit.Pixel },
                    right: { value: 0, unit: Unit.Pixel },
                    bottom: { value: 0, unit: Unit.Pixel },
                    left: { value: 0, unit: Unit.Pixel },
                },
                align: {
                    horizontal: LcAlign.LEFT,
                    vertical: LcVerticalAlign.Top,
                },
            },
            events: [{
                type: LcEventType.OnClick,
                target: { type: LcSectionType.LcAnalytics },
                data: {},
            }],
            grid: {
                x: 4,
                y: 1,
                width: 6,
                height: 2,
            },
            componentProps: null,
            sectionId: '',
        };

        wrapper = mount(<LcGridBlockText {...props} />);

        expect(wrapper.find('.lc-grid-block-text').childAt(0).prop('events')).toEqual([{
            type: LcEventType.OnClick,
            target: { type: LcSectionType.LcAnalytics },
            data: {},
        }]);
    });
});
