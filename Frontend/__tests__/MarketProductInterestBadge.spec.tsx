import * as React from 'react';
import { shallow } from 'enzyme';
import { MarketProductInterestBadge } from '../MarketProductInterestBadge';
import { defaultData } from './datastub';

describe('MarketProductInterestBadge', () => {
    it('должен отрендериться без падения', () => {
        const wrapper = shallow(<MarketProductInterestBadge {...defaultData} />);
        expect(wrapper.length).toEqual(1);
    });

    describe('отоброжение количества просмотров', () => {
        it('должен отобразить: 1 человек интересовался за 2 месяца', () => {
            const wrapper = shallow(<MarketProductInterestBadge viewedReason={Object.assign({}, defaultData.viewedReason, { value: 1 })} />);
            expect(wrapper.render().text()).toEqual('1\u00a0человек интересовался за 2 месяца');
        });

        it('должен отобразить: 2 человека интересовались за 2 месяца', () => {
            const wrapper = shallow(<MarketProductInterestBadge viewedReason={Object.assign({}, defaultData.viewedReason, { value: 2 })} />);
            expect(wrapper.render().text()).toEqual('2\u00a0человека интересовались за 2 месяца');
        });

        it('должен отобразить: 5 человек интересовались за 2 месяца', () => {
            const wrapper = shallow(<MarketProductInterestBadge viewedReason={Object.assign({}, defaultData.viewedReason, { value: 5 })} />);
            expect(wrapper.render().text()).toEqual('5\u00a0человек интересовались за 2 месяца');
        });
    });
    describe('отоброжение количества покупок', () => {
        it('должен отобразить: 1 покупка за 2 месяца', () => {
            const wrapper = shallow(<MarketProductInterestBadge boughtReason={Object.assign({}, defaultData.boughtReason, { value: 1 })} />);
            expect(wrapper.render().text()).toEqual('1\u00a0покупка за 2 месяца');
        });

        it('должен отобразить: 2 покупки за 2 месяца', () => {
            const wrapper = shallow(<MarketProductInterestBadge boughtReason={Object.assign({}, defaultData.boughtReason, { value: 2 })} />);
            expect(wrapper.render().text()).toEqual('2\u00a0покупки за 2 месяца');
        });

        it('должен отобразить: 5 покупок за 2 месяца', () => {
            const wrapper = shallow(<MarketProductInterestBadge boughtReason={Object.assign({}, defaultData.boughtReason, { value: 5 })} />);
            expect(wrapper.render().text()).toEqual('5\u00a0покупок за 2 месяца');
        });
    });
});
