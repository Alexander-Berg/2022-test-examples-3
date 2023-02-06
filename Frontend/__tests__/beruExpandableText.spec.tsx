import * as React from 'react';
import { shallow } from 'enzyme';
import { BeruExpandableText, IProps } from '../BeruExpandableText';
import * as stubData from './datastub';

describe('Компонент BeruExpandableText', () => {
    const data = stubData.dataDefault as IProps;

    it('должен отрендериться без ошибок', () => {
        const wrapper = shallow(<BeruExpandableText {...data} />);
        expect(wrapper.length).toEqual(1);
    });

    it('должен содержать кнопку "..."', () => {
        const wrapper = shallow(<BeruExpandableText {...data} />);
        expect(wrapper.find('.beru-expandable-text__more-button').exists()).toEqual(true);
    });

    it('должен содержать неполный текст', () => {
        const wrapper = shallow(<BeruExpandableText {...data} />);
        expect(wrapper.render().text().length).toBeLessThan((data.maxLength || 200) + 3);
    });

    it('по клику на кнопку должен сскрыть её', () => {
        const wrapper = shallow(<BeruExpandableText {...data} />);
        wrapper.find('.beru-expandable-text__more-button').simulate('click');
        expect(wrapper.find('.beru-expandable-text__more-button').exists()).toEqual(false);
    });

    it('по клику на кнопку должен отобразить весь текст', () => {
        const wrapper = shallow(<BeruExpandableText {...data} />);
        wrapper.find('.beru-expandable-text__more-button').simulate('click');
        expect(wrapper.render().text()).toEqual(data.children);
    });
});
