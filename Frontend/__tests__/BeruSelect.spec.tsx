import * as React from 'react';
import { shallow, mount } from 'enzyme';
import { BeruPopup } from '@yandex-turbo/components/BeruPopup/BeruPopup';
import { BeruText } from '@yandex-turbo/components/BeruText/BeruText';
import { LinkLayoutDefault as Link } from '@yandex-turbo/components/Link/_layout/Link_layout_default';
import { BeruSelect } from '../BeruSelect';
import { TOption } from '../BeruSelect.types';

describe('BeruSelect', () => {
    let options: TOption[];

    beforeEach(() => {
        options = [
            { value: '1', title: 'Опция 1', active: false },
            { value: '2', title: 'Опция 2', active: false },
            { value: '3', title: 'Опция 3', active: false },
        ];
    });

    it('должен корректно отрисовываться если не передана предвыбранная опция', () => {
        const wrapper = shallow(<BeruSelect options={options} placeholder={'Выбери меня'} />);

        // Триггер должен отображать текст переданный в поле placeholder
        expect(wrapper.find('button.beru-select__button').text()).toEqual('Выбери меня');
        // Дропдаун не должен быть видим
        expect(wrapper.state()).toEqual({ showDropDown: false });
        expect(wrapper.find(BeruPopup).props().isOpen).toBe(false);
    });

    it('должен корректно отрисовываться если передана активная опция', () => {
        // сделаем одну из опций активной
        options[1].active = true;

        const wrapper = shallow(<BeruSelect options={options} placeholder={'Выбери меня'} />);

        // Триггер должен должна отображать текст активной опции
        expect(wrapper.find('button.beru-select__button').text()).toEqual(options[1].title);
        // В стейт должна записаться выбранная опция и сотояние быть свернутое
        expect(wrapper.state()).toEqual({
            showDropDown: false,
            selectedOption: {
                ...options[1],
            },
        });
        // Дропдаун не должен быть видим
        expect(wrapper.find(BeruPopup).props().isOpen).toBe(false);

        // Визуально должна быть активна только одна опция
        const option = wrapper.find(BeruPopup).find('.beru-select__option_active');

        expect(option).toHaveLength(1);
        expect(option.find(BeruText).props().children).toEqual(options[1].title);
    });

    it('должен корректно отрисовываться если передано несколько активных опций', () => {
        // сделаем несколько опций активными
        options[0].active = true;
        options[2].active = true;

        const wrapper = shallow(<BeruSelect options={options} placeholder={'Выбери меня'} />);

        // Триггер должен отображать текст активной опции(если их несколько, то берется первая)
        expect(wrapper.find('button.beru-select__button').text()).toEqual(options[0].title);

        // В стейт должна записаться выбранная опция и сотояние быть свернутое
        expect(wrapper.state()).toEqual({
            showDropDown: false,
            selectedOption: {
                ...options[0],
            },
        });

        // Дропдаун не должен быть видим
        expect(wrapper.find(BeruPopup).props().isOpen).toBe(false);

        // Визуально должна быть активна только одна опция
        const option = wrapper.find(BeruPopup).find('.beru-select__option_active');

        expect(option).toHaveLength(1);
        expect(option.find(BeruText).props().children).toEqual(options[0].title);
    });

    it('При клике по тригеру должен открываться и закрываться дропдаун', () => {
        const wrapper = shallow(<BeruSelect options={options} placeholder={'Выбери меня'} />);

        // Убедимся что дропдаун скрыт
        expect(wrapper.find(BeruPopup).props().isOpen).toEqual(false);

        //кликнем по триггеру
        wrapper.find('.beru-select__button').simulate('click');

        // Должен быть открыт
        expect(wrapper.state()).toMatchObject({ showDropDown: true });
        expect(wrapper.find(BeruPopup).props().isOpen).toEqual(true);

        // еще раз кликаем
        wrapper.find('.beru-select__button').simulate('click');

        // Должен быть закрыт
        expect(wrapper.state()).toMatchObject({ showDropDown: false });
        expect(wrapper.find(BeruPopup).props().isOpen).toEqual(false);
    });

    it('триггер должен передаваться в качестве якоря для показа дропдауна', () => {
        const wrapper = mount(<BeruSelect options={options} placeholder={'Выбери меня'} />);

        expect(wrapper.find(BeruPopup).props().anchor.current!.className).toEqual('beru-select__button');
    });

    it('должен корректно отрисовывать опции являющиеся ссылками', () => {
        // Делаем опции ссылками
        options[1].url = 'https://yandex.ru';
        options[2].url = 'https://m.yandex.ru';

        const wrapper = shallow(<BeruSelect options={options} placeholder={'Выбери меня'} />);
        const opt = wrapper.find(BeruPopup).find(Link);

        expect(opt).toHaveLength(2);
        opt.map((link, i) => {
            expect(link.props()).toMatchObject({
                type: 'text',
                url: i === 0 ? options[1].url : options[2].url,
            });
        });
    });
});
