import 'jest';
import * as React from 'react';
import * as enzyme from 'enzyme';

import { RadioItem } from '../RadioItem';
import { ICommonRadioItemProps } from '../../../CartForm.types';

describe('RadioItem', () => {
    /**
     * Pairwise snapshots
     *
     * label, meta, activeValue, value,
     * name, onChange, onChangeValue, uniq,
     * className, view, renderAfter
    */
    describe('Соответствует snapshot', () => {
        it('view="checkbox", active', () => {
            const onChange = jest.fn();
            const props: ICommonRadioItemProps = {
                activeValue: 'value',
                value: 'value',
                label: 'label',
                name: 'name',
                view: 'checkbox',
                uniq: 'uniq',
                onChange,
                onChangeValue: 'onChangeValue',
            };
            const shallow = enzyme.shallow(<RadioItem {...props} />);
            expect(shallow).toMatchSnapshot();
        });

        it('view="circle", not active', () => {
            const onChange = jest.fn();
            const props: ICommonRadioItemProps = {
                activeValue: 'value1',
                value: 'value',
                label: 'label',
                name: 'name',
                view: 'circle',
                uniq: 'uniq',
                onChange,
                onChangeValue: 'onChangeValue',
            };
            const shallow = enzyme.shallow(<RadioItem {...props} />);
            expect(shallow).toMatchSnapshot();
        });

        it('Расположение блоков из renderProps', () => {
            const onChange = jest.fn();
            const renderAfter = () => 'renderAfter';
            const props: ICommonRadioItemProps = {
                value: 'value',
                label: 'label',
                name: 'name',
                view: 'checkbox',
                uniq: 'uniq',
                onChange,
                onChangeValue: 'onChangeValue',
                renderAfter,
            };
            const shallow = enzyme.shallow(<RadioItem {...props} />);
            expect(shallow).toMatchSnapshot();
        });
    });

    it('Вызывает колбек onChange при клике на label', () => {
        const onChange = jest.fn();
        const onChangeValue = 'onChangeValue';
        const uniq = 'uniq';
        const props: ICommonRadioItemProps = {
            value: 'value',
            label: 'label',
            name: 'name',
            view: 'checkbox',
            uniq,
            onChange,
            onChangeValue,
        };
        const shallow = enzyme.shallow(<RadioItem {...props} />);
        const label = shallow.find('label');
        label.simulate('click');
        expect(onChange).toHaveBeenCalledTimes(1);
        expect(onChange).toHaveBeenCalledWith({ value: onChangeValue, uniq });
    });
});
