import * as React from 'react';
import { shallow } from 'enzyme';
import { Sale } from '../Sale';

const defaultData = {
    value: 'Скидка 55%',
};

const additionalData = {
    cls: 'hello-world',
};

describe('Sale', () => {
    it('рендерится без ошибок', () => {
        const wrapper = shallow(
            <Sale {...defaultData} />
        );
        expect(wrapper.length).toEqual(1);
    });

    it('добавляет переданный класс', () => {
        const wrapper = shallow(<Sale {...defaultData} className={additionalData.cls} />);
        expect(wrapper.find('.turbo-sale').hasClass('hello-world')).toEqual(true);
    });
});
