import * as React from 'react';
import { shallow } from 'enzyme';
import { LcSection, IProps } from '../LcSection';
import * as stubData from '../datastub';

describe('Компонент LcSection', () => {
    const data = stubData.sectionWithAnchor as IProps;
    const section = shallow(<LcSection {...data} />);

    it('должен отрендериться', () => {
        expect(section.length).toEqual(1);
    });

    it('добаляет модификатор для секций во всю ширину экрана', () => {
        expect(section.hasClass('lc-section_is-expanded')).toEqual(true);
    });

    it('добавляет атрибуты для превью конструктора', () => {
        const ethalonAttrs = stubData.sectionWithAnchor.previewAttrs;

        expect(section.prop('data-section-id')).toEqual(ethalonAttrs['data-section-id']);
        expect(section.prop('data-section-name')).toEqual(ethalonAttrs['data-section-name']);
    });

    it('добавляет примиксованый класс', () => {
        expect(section.hasClass(stubData.sectionWithAnchor.className)).toEqual(true);
    });

    it('добавляет в id проброшеный якорь', () => {
        expect(section.prop('id')).toEqual(stubData.sectionWithAnchor.anchor);
    });
});
