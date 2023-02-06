import * as React from 'react';
import { shallow } from 'enzyme';
import * as serializer from 'jest-serializer-html';

import { LcOffsets, IProps } from '../LcOffsets';
import { LcSizes } from '../../lcTypes/lcTypes';

const baseProps: IProps = {
    padding: {
        top: LcSizes.M,
        bottom: LcSizes.M,
    },
};

expect.addSnapshotSerializer(serializer);

describe('LcOffsets', () => {
    it('должен отрендериться', () => {
        const lcOffsets = shallow(<LcOffsets {...baseProps} />);

        expect(lcOffsets.html()).toMatchSnapshot();
    });

    it('должен содержать стилевые классы', () => {
        const lcOffsets = shallow(<LcOffsets {...baseProps} />);

        expect(lcOffsets.hasClass('lc-offsets_padding-top_m')).toBe(true);
        expect(lcOffsets.hasClass('lc-offsets_padding-bottom_m')).toBe(true);
    });

    describe('если передан paddings', () => {
        const paddings = '2px 0px 10px 3px';
        const propsWithPaddings = {
            ...baseProps,
            paddings,
        };

        it('должен отрендериться', () => {
            const lcOffsets = shallow(<LcOffsets {...propsWithPaddings} />);

            expect(lcOffsets.html()).toMatchSnapshot();
        });

        it('должен отрендериться с инлайн отступами', () => {
            const lcOffsets = shallow(<LcOffsets {...propsWithPaddings} />);

            expect(lcOffsets.prop('style')).toEqual({ padding: '2px 0px 10px 3px' });
        });

        it('должен отрендериться без стилевых классов', () => {
            const lcOffsets = shallow(<LcOffsets {...propsWithPaddings} />);

            expect(lcOffsets.hasClass('lc-offsets_padding-top_m')).toBe(false);
            expect(lcOffsets.hasClass('lc-offsets_padding-bottom_m')).toBe(false);
        });
    });
});
