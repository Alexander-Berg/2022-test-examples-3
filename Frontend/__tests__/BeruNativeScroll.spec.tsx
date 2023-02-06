import * as React from 'react';
import { shallow } from 'enzyme';
import { BeruNativeScroll } from '../BeruNativeScroll';

const mockGenerateId = jest.fn();
jest.mock('@yandex-turbo/core/uniq', () => {
    return {
        generateId: () => {
            return mockGenerateId();
        },
    };
});

function getItem() {
    return <div>item</div>;
}

describe('BeruNativeScroll', () => {
    it('должен отрендериться без ошибок', () => {
        const wrapper = shallow(<BeruNativeScroll>{getItem()}</BeruNativeScroll>);

        expect(wrapper.length).toEqual(1);
    });

    it('переданные class names должны выставляться на конкретные DOM ноды', () => {
        const wrapper = shallow(<BeruNativeScroll className="test" itemClassName="test__item">{getItem()}</BeruNativeScroll>);

        expect(wrapper.hasClass('test')).toEqual(true);
        expect(wrapper.find('.beru-native-scroll__item').hasClass('test__item')).toEqual(true);
    });
});
