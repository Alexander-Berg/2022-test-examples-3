import * as React from 'react';
import { shallow } from 'enzyme';
import { BeruText } from '@yandex-turbo/components/BeruText/BeruText';
import { BeruDisclaimer, IAgeProps } from '../BeruDisclaimer';

function ageTestAssert(age: IAgeProps['age']) {
    const wrapper = shallow(<BeruDisclaimer age={age} text="Text" />);

    expect(wrapper.find(BeruText).props()).toEqual({
        className: `beru-disclaimer beru-disclaimer_age_${age}`,
        size: '100',
        theme: 'muted',
        children: 'Text',
    });
}

describe('BeruDisclaimer', () => {
    describe('Возрастной дисклеймер', () => {
        it('должен корректно отрисовываться для 0+', () => {
            ageTestAssert('0');
        });

        it('должен корректно отрисовываться для 6+', () => {
            ageTestAssert('6');
        });

        it('должен корректно отрисовываться для 12+', () => {
            ageTestAssert('12');
        });

        it('должен корректно отрисовываться для 16+', () => {
            ageTestAssert('16');
        });

        it('должен корректно отрисовываться для 18+', () => {
            ageTestAssert('18');
        });
    });

    describe('Опасные лекарства дисклеймер', () => {
        it('должен корректно отрисовываться', () => {
            const wrapper = shallow(<BeruDisclaimer isMedicine text="Text" />);

            expect(wrapper.find(BeruText).props()).toEqual({
                className: 'beru-disclaimer beru-disclaimer_medicine',
                size: '100',
                theme: 'muted',
                children: 'Text',
            });
        });
    });

    describe('Базовый дисклеймер', () => {
        it('должен корректно отрисовываться', () => {
            const wrapper = shallow(<BeruDisclaimer text="Text" />);

            expect(wrapper.find(BeruText).props()).toEqual({
                className: 'beru-disclaimer',
                size: '100',
                theme: 'muted',
                children: 'Text',
            });
        });
    });
});
