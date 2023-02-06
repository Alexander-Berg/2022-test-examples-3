import * as React from 'react';
import { shallow } from 'enzyme';
import { BeruImage } from '../BeruImage';

describe('BeruImage', () => {
    const image = 'path/to/image';

    it('должен отрисовываться без ошибок', () => {
        const wrapper = shallow(<BeruImage src={image} />);

        expect(wrapper.length).toEqual(1);
    });

    it('должен применяться правильный модификатор', () => {
        const wrapper = shallow(<BeruImage src={image} fillParent />);

        expect(wrapper.hasClass('beru-image_full-size')).toBe(true);
    });

    describe('Элемент "image-box"', () => {
        it('должен правильно собирать атрибут style если "maxSize=number"', () => {
            const size = 150;
            const wrapper = shallow(<BeruImage src={image} maxSize={size} />);

            expect(wrapper.find('.beru-image__image-box')
                .prop('style')
            ).toEqual({
                backgroundImage: `url(${image})`,
                backgroundSize: 'contain',
                maxWidth: `${size}px`,
                maxHeight: `${size}px`,
            });
        });

        it('должен правильно собирать атрибут style если "maxSize=object"', () => {
            const size = { maxWidth: 150, maxHeight: 250 };
            const wrapper = shallow(<BeruImage src={image} maxSize={size} />);

            expect(wrapper.find('.beru-image__image-box')
                .prop('style')
            ).toEqual({
                backgroundImage: `url(${image})`,
                backgroundSize: 'contain',
                maxWidth: `${size.maxWidth}px`,
                maxHeight: `${size.maxHeight}px`,
            });
        });

        it('должен правильно собирать атрибут style если "maxSize" не передан', () => {
            const wrapper = shallow(<BeruImage src={image} />);

            expect(wrapper.find('.beru-image__image-box')
                .prop('style')
            ).toEqual({
                backgroundImage: `url(${image})`,
                backgroundSize: 'auto',
            });
        });
    });

    describe('Элемент "image"', () => {
        it('должен правильно собирать атрибут style если "maxSize=number"', () => {
            const size = 150;
            const wrapper = shallow(<BeruImage src={image} maxSize={size} />);

            expect(wrapper.find('.beru-image__image')
                .prop('style')
            ).toEqual({
                maxWidth: `${size}px`,
                maxHeight: `${size}px`,
            });
        });

        it('должен правильно собирать атрибут style если "maxSize=object"', () => {
            const size = { maxWidth: 150, maxHeight: 250 };
            const wrapper = shallow(<BeruImage src={image} maxSize={size} />);

            expect(wrapper.find('.beru-image__image')
                .prop('style')
            ).toEqual({
                maxWidth: `${size.maxWidth}px`,
                maxHeight: `${size.maxHeight}px`,
            });
        });

        it('должен правильно собирать атрибут style если "maxSize" не передан', () => {
            const wrapper = shallow(<BeruImage src={image} />);

            expect(wrapper.find('.beru-image__image')
                .prop('style')
            ).toEqual({});
        });
    });
});
