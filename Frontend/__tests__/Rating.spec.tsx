import * as React from 'react';
import { mount, shallow } from 'enzyme';

import { Rating } from '..';

describe('Rating', () => {
    it('не должен рендерится, если значение 0', () => {
        const wrapper = shallow(<Rating value={0} />);
        expect(wrapper.isEmptyRender()).toBeTruthy();
    });

    describe('Вычисление grade', () => {
        it('должен добавить класс _grade_1 если значение меньше или равно 1', () => {
            const wrapper = mount(<Rating value={1} />);

            expect(wrapper.find('.Rating').hasClass('Rating_grade_2')).toEqual(false);
            expect(wrapper.find('.Rating').hasClass('Rating_grade_3')).toEqual(false);
            expect(wrapper.find('.Rating').hasClass('Rating_grade_4')).toEqual(false);
            expect(wrapper.find('.Rating').hasClass('Rating_grade_5')).toEqual(false);
            expect(wrapper.find('.Rating').hasClass('Rating_grade_1')).toBeTruthy();
        });

        it('должен добавить класс _grade_2 если значение больше 1', () => {
            const wrapper = mount(<Rating value={1.1} />);

            expect(wrapper.find('.Rating').hasClass('Rating_grade_1')).toEqual(false);
            expect(wrapper.find('.Rating').hasClass('Rating_grade_3')).toEqual(false);
            expect(wrapper.find('.Rating').hasClass('Rating_grade_4')).toEqual(false);
            expect(wrapper.find('.Rating').hasClass('Rating_grade_5')).toEqual(false);
            expect(wrapper.find('.Rating').hasClass('Rating_grade_2')).toBeTruthy();
        });

        it('должен добавить класс _grade_3 если значение больше 2', () => {
            const wrapper = mount(<Rating value={2.1} />);

            expect(wrapper.find('.Rating').hasClass('Rating_grade_1')).toEqual(false);
            expect(wrapper.find('.Rating').hasClass('Rating_grade_2')).toEqual(false);
            expect(wrapper.find('.Rating').hasClass('Rating_grade_4')).toEqual(false);
            expect(wrapper.find('.Rating').hasClass('Rating_grade_5')).toEqual(false);
            expect(wrapper.find('.Rating').hasClass('Rating_grade_3')).toBeTruthy();
        });

        it('должен добавить класс _grade_4 если значение больше 3.5', () => {
            const wrapper = mount(<Rating value={3.77} />);

            expect(wrapper.find('.Rating').hasClass('Rating_grade_1')).toEqual(false);
            expect(wrapper.find('.Rating').hasClass('Rating_grade_2')).toEqual(false);
            expect(wrapper.find('.Rating').hasClass('Rating_grade_3')).toEqual(false);
            expect(wrapper.find('.Rating').hasClass('Rating_grade_5')).toEqual(false);
            expect(wrapper.find('.Rating').hasClass('Rating_grade_4')).toBeTruthy();
        });

        it('должен добавить класс _grade_5 если значение больше 4.5', () => {
            const wrapper = mount(<Rating value={4.6} />);

            expect(wrapper.find('.Rating').hasClass('Rating_grade_1')).toEqual(false);
            expect(wrapper.find('.Rating').hasClass('Rating_grade_2')).toEqual(false);
            expect(wrapper.find('.Rating').hasClass('Rating_grade_3')).toEqual(false);
            expect(wrapper.find('.Rating').hasClass('Rating_grade_4')).toEqual(false);
            expect(wrapper.find('.Rating').hasClass('Rating_grade_5')).toBeTruthy();
        });
    });
});
