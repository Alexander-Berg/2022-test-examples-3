import * as React from 'react';
import { shallow } from 'enzyme';
import { BeruDeliveryInterval } from '../BeruDeliveryInterval';

const mockToLocaleString = jest.fn().mockImplementation(() => (''));
let mainToLocaleString: typeof Date.prototype.toLocaleString;
let RealDate: typeof Date;

describe('BeruDeliveryInterval', () => {
    beforeEach(() => {
        mainToLocaleString = Date.prototype.toLocaleString;
        Date.prototype.toLocaleString = mockToLocaleString;

        mockToLocaleString.mockClear();

        // Mock Date object
        RealDate = global.Date;
        // @ts-ignore
        global.Date = jest.fn(() => new RealDate(RealDate.UTC(2019, 1, 30, 15, 0)));
    });

    afterEach(() => {
        Date.prototype.toLocaleString = mainToLocaleString;

        // Restore Date
        global.Date = RealDate;
    });

    it('должен выводить "сегодня, {date} {month}"', () => {
        const wrapper = shallow(<BeruDeliveryInterval maxDays={0} minDays={0} />);

        expect(mockToLocaleString).toHaveBeenCalledWith('ru', { day: 'numeric' });
        expect(mockToLocaleString).toHaveBeenCalledWith('ru', { month: 'long' });
        expect(wrapper.text()).toMatch(/^сегодня,.*/);
    });

    it('должен выводить "Завтра, {weekDay}"', () => {
        const wrapper = shallow(<BeruDeliveryInterval minDays={1} maxDays={1} />);

        expect(mockToLocaleString).toHaveBeenCalledWith('ru', { day: 'numeric' });
        expect(mockToLocaleString).toHaveBeenCalledWith('ru', { month: 'long' });
        expect(wrapper.text()).toMatch(/^завтра,.*/);
    });

    it('должен выводить "в {weekDay}, {date} {month}"', () => {
        const wrapper = shallow(<BeruDeliveryInterval minDays={2} maxDays={2} />);

        expect(mockToLocaleString).toHaveBeenCalledWith('ru', { weekday: 'long' });
        expect(mockToLocaleString).toHaveBeenCalledWith('ru', { day: 'numeric' });
        expect(mockToLocaleString).toHaveBeenCalledWith('ru', { month: 'long' });
        expect(wrapper.text()).toMatch(/(во|в)\s/);
    });

    it('должен выводить "{date} {month}"', () => {
        shallow(<BeruDeliveryInterval minDays={10} maxDays={10} />);

        expect(mockToLocaleString).toHaveBeenCalledWith('ru', { day: 'numeric' });
        expect(mockToLocaleString).toHaveBeenCalledWith('ru', { month: 'long' });
    });

    it('должен выводить "до {date} {month}"', () => {
        const wrapper = shallow(<BeruDeliveryInterval minDays={0} maxDays={1} />);

        expect(mockToLocaleString).toHaveBeenCalledWith('ru', { day: 'numeric' });
        expect(mockToLocaleString).toHaveBeenCalledWith('ru', { month: 'long' });
        expect(wrapper.text()).toMatch(/^до.*/);
    });

    it('должен выводить "после {date} {month}"', () => {
        const wrapper = shallow(<BeruDeliveryInterval minDays={1} maxDays={0} />);

        expect(mockToLocaleString).toHaveBeenCalledWith('ru', { day: 'numeric' });
        expect(mockToLocaleString).toHaveBeenCalledWith('ru', { month: 'long' });
        expect(wrapper.text()).toMatch(/^после.*/);
    });

    it('должен выводить "{date}-{date} {month}"', () => {
        const wrapper = shallow(<BeruDeliveryInterval minDays={2} maxDays={3} />);

        expect(mockToLocaleString).toHaveBeenCalledWith('ru', { day: 'numeric' });
        expect(mockToLocaleString).toHaveBeenCalledWith('ru', { month: 'long' });
        expect(mockToLocaleString).toHaveBeenCalledTimes(3);
        expect(wrapper.text()).toMatch(/.*–.*/);
    });

    it('должен выводить "{date} {month}-{date} {month}"', () => {
        const wrapper = shallow(<BeruDeliveryInterval minDays={30} maxDays={60} />);

        expect(mockToLocaleString).toHaveBeenCalledWith('ru', { day: 'numeric' });
        expect(mockToLocaleString).toHaveBeenCalledWith('ru', { month: 'long' });
        expect(mockToLocaleString).toHaveBeenCalledTimes(4);
        expect(wrapper.text()).toMatch(/.*–.*/);
    });

    it('должен ничего не выводить {minDays = -1}', () => {
        const wrapper = shallow(<BeruDeliveryInterval minDays={-1} maxDays={1} />);

        expect(mockToLocaleString).not.toHaveBeenCalled();
        expect(wrapper.text()).toEqual('');
    });

    it('должен ничего не выводить {maxDays = -1}', () => {
        const wrapper = shallow(<BeruDeliveryInterval minDays={1} maxDays={-1} />);

        expect(mockToLocaleString).not.toHaveBeenCalled();
        expect(wrapper.text()).toEqual('');
    });
});
