import * as React from 'react';
import { shallow } from 'enzyme';
import { AmountPicker } from '../AmountPicker';

class AmountPickerParent extends React.Component {
    public render(): React.ReactNode {
        return (
            <AmountPicker
                value={1}
                name="amount-picker"
                className="Card"
            />
        );
    }
}

describe('AmountPicker component', () => {
    it('should render without crashing', () => {
        const wrapper = shallow(
            <AmountPickerParent />
        );
        expect(wrapper.length).toEqual(1);
    });

    it('должен отправлять событие при нажатии на кнопку прибавления количества', () => {
        const onChange = jest.fn();

        const picker = shallow(<AmountPicker
            value={1}
            name="count"
            onChange={onChange}
        />);
        picker.find('.turbo-amount-picker__btn_type_inc').simulate('click');
        expect(onChange.mock.calls.length).toEqual(1);
        expect(onChange.mock.calls[0]).toMatchObject([{
            target: { name: 'count', value: 2, delta: 1 },
        }]);
    });

    it('должен отправлять событие при нажатии на кнопку уменьшения количества', () => {
        const onChange = jest.fn();

        const picker = shallow(<AmountPicker
            value={2}
            name="count"
            onChange={onChange}
        />);
        picker.find('.turbo-amount-picker__btn_type_dec').simulate('click');
        expect(onChange.mock.calls.length).toEqual(1);
        expect(onChange.mock.calls[0]).toMatchObject([{
            target: { name: 'count', value: 1, delta: -1 },
        }]);
    });

    it('должен отправлять событие при изменении значения путём прямого ввода в инпут', () => {
        const onChange = jest.fn();

        const picker = shallow(<AmountPicker
            value={1}
            name="count"
            onChange={onChange}
        />);
        picker.find('.turbo-amount-picker__input').simulate('change', { target: { value: 5 } });
        expect(onChange.mock.calls.length).toEqual(1);
        expect(onChange.mock.calls[0]).toMatchObject([{
            target: { name: 'count', value: 5, delta: 4 },
        }]);
    });
});
