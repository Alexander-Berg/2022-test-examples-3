import * as React from 'react';
import { shallow } from 'enzyme';
import * as colorUtils from '@yandex-turbo/core/utils/color';
import { BeruCheckboxIndicator } from '../Indicator/BeruCheckboxIndicator';
import { Color } from '../BeruCheckbox.types';

describe('BeruCheckboxIndicator', () => {
    let hex2rgb: ReturnType<typeof jest.spyOn>;
    let isLight: ReturnType<typeof jest.spyOn>;
    let isWhite: ReturnType<typeof jest.spyOn>;

    beforeEach(() => {
        hex2rgb = jest.spyOn(colorUtils, 'hex2rgb');
        isLight = jest.spyOn(colorUtils, 'isLight');
        isWhite = jest.spyOn(colorUtils, 'isWhite');
    });

    it('по умолчанию отрисовывается без ошибок', () => {
        const wrapper = shallow(<BeruCheckboxIndicator />);

        expect(hex2rgb).not.toHaveBeenCalled();
        expect(isLight).not.toHaveBeenCalled();
        expect(isWhite).not.toHaveBeenCalled();
        expect(wrapper.html()).toEqual('<span class="beru-checkbox__indicator"></span>');
    });

    it('корректно отрисовывается если цвет multicolor', () => {
        const wrapper = shallow(<BeruCheckboxIndicator color={Color.MULTICOLOR} />);

        expect(hex2rgb).not.toHaveBeenCalled();
        expect(isLight).not.toHaveBeenCalled();
        expect(isWhite).not.toHaveBeenCalled();
        expect(wrapper.html()).toEqual('<span class="beru-checkbox__indicator beru-checkbox__indicator_multicolor"></span>');
    });

    it('корректно отрисовывается если цвет в hex формате', () => {
        const rgb = { r: 255, g: 255, b: 255 };

        hex2rgb.mockReturnValue(rgb);
        isLight.mockReturnValue(true);
        isWhite.mockReturnValue(true);

        const wrapper = shallow(<BeruCheckboxIndicator color="#ffffff" />);

        expect(hex2rgb).toHaveBeenCalledWith(expect.stringMatching('#ffffff'));
        expect(isLight).toHaveBeenCalledWith(expect.objectContaining(rgb));
        expect(isWhite).toHaveBeenCalledWith(expect.objectContaining(rgb));
        expect(wrapper.html()).toEqual('<span style="background:#ffffff" class="beru-checkbox__indicator beru-checkbox__indicator_dark beru-checkbox__indicator_white"></span>');
    });
});
