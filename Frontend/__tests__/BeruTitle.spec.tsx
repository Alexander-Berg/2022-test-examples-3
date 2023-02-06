import * as React from 'react';
import { shallow } from 'enzyme';
import { BeruText } from '@yandex-turbo/components/BeruText/BeruText';
import { BeruTitle, IProps } from '../BeruTitle';
import * as stubData from '../datastub';

describe('Компонент BeruTitle', () => {
    it('должен отрендериться без ошибок', () => {
        const data = stubData.dataDefault as IProps;
        const wrapper = shallow(<BeruTitle {...data} />);
        expect(wrapper.length).toEqual(1);
    });

    describe('с данными из стаба (dataBigH1)', () => {
        const data = stubData.dataBigH1 as IProps;

        it('должен содержать переданный текст', () => {
            const wrapper = shallow(<BeruTitle {...data} />);
            expect(wrapper.find('.beru-title').render().text()).toEqual(data.children);
        });

        it('Должен использовать BeruText с указанными параметрами', () => {
            const wrapper = shallow(<BeruTitle {...data} />);
            const { size, theme, weight, children } = wrapper.find(BeruText).props();
            expect(size).toEqual(data.size);
            expect(theme).toEqual(data.theme);
            expect(weight).toEqual('medium');
            expect(children).toEqual(data.children);
        });

        it('должен содержать тег h1', () => {
            const wrapper = shallow(<BeruTitle {...data} />);
            expect(wrapper.find('h1').length).toEqual(1);
        });

        it('должен содержать тег h2', () => {
            const wrapper = shallow(<BeruTitle {...data} tag={'h2'} />);
            expect(wrapper.find('h2').length).toEqual(1);
        });
    });
});
