import * as React from 'react';
import { mount } from 'enzyme';
import * as serializer from 'jest-serializer-html';
import { LcFont, LcSeoTag, LcSizePx, LcTypeface } from '@yandex-turbo/components/lcTypes/lcTypes';
import { LcTextBlock } from '../LcTextBlock';

expect.addSnapshotSerializer(serializer);

describe('LcTextBlock component', () => {
    describe('SEO tag', () => {
        const props = {
            size: LcSizePx.s16,
            font: LcFont.TEXT,
            typeface: LcTypeface.REGULAR,
            color: 'rgba(0, 0, 0, 1)',
            lineHeight: 22,
        };

        Object.entries(LcSeoTag).forEach(([key, tag]: [string, LcSeoTag]) => {
            test(`should render LcSeoTag.${key}`, () => {
                const lcTextBlock = mount(<LcTextBlock {...props} tag={tag} />);

                expect(lcTextBlock.html()).toMatchSnapshot();
            });
        });
    });
});
