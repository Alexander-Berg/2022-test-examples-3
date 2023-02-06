import * as React from 'react';
import { shallow } from 'enzyme';
import { BeruText, IProps } from '../BeruText';
import * as stubData from './datastub';

describe('Компонент BeruText', () => {
    it('должен отрендериться без ошибок', () => {
        const data = stubData.dataDefault as IProps;
        const wrapper = shallow(<BeruText {...data} />);
        expect(wrapper.length).toEqual(1);
    });

    describe('с данными из стаба (dataBigWarning)', () => {
        const data = stubData.dataBigWarning as IProps;

        it('должен содержать переданный текст', () => {
            const wrapper = shallow(<BeruText {...data} />);
            expect(wrapper.text()).toEqual(data.children);
        });

        it('должен иметь класс с указанным размером', () => {
            const wrapper = shallow(<BeruText {...data} />);
            expect(wrapper.hasClass('beru-text_size_700')).toEqual(true);
        });

        it('должен иметь класс с указанной темой', () => {
            const wrapper = shallow(<BeruText {...data} />);
            expect(wrapper.hasClass('beru-text_theme_warning')).toEqual(true);
        });

        it('должен иметь класс с указанной толщиной текста', () => {
            const wrapper = shallow(<BeruText {...data} />);
            expect(wrapper.hasClass('beru-text_weight_bold')).toEqual(true);
        });

        it('должен иметь id атрибут', () => {
            const wrapper = shallow(<BeruText {...data} />);

            expect(wrapper.props()).toMatchObject({ id: 'custom-id' });
        });
    });
});
