import { shallow } from 'enzyme';
import * as React from 'react';

import { DUTY_STATE } from '../../../../constants';
import { deepCopy } from '../../../../utils/utils';
import { CallsNotify } from '../component';
import { LastPerformerItem, LastPerformers } from '../LastPerformers';
import { messages } from './messages.mock';
import { performer } from './performer.mock';

describe('Calls Notifier', () => {
    let wrapper;

    it('empty call', () => {
        wrapper = shallow(<CallsNotify calls={null} tags={null} users={null}/>);
        expect(wrapper).toMatchSnapshot();
    });

    it('new call', () => {
        wrapper = shallow(<CallsNotify calls={messages.calls} tags={messages.tags} users={messages.users}/>);
        expect(wrapper).toMatchSnapshot();
    });

    it('menu open /close (by close btn)', () => {
        wrapper = shallow(<CallsNotify calls={messages.calls} tags={messages.tags} users={messages.users}/>);
        wrapper.find('.component').simulate('click');
        expect(wrapper).toMatchSnapshot();
        wrapper.find('.close').simulate('click');
        expect(wrapper).toMatchSnapshot();
    });

    it('menu open /close (client)', () => {
        wrapper = shallow(<CallsNotify calls={messages.calls} tags={messages.tags} users={messages.users}/>);
        wrapper.find('.component').simulate('click');
        expect(wrapper).toMatchSnapshot();
        wrapper.find('.component').simulate('click');
        expect(wrapper).toMatchSnapshot();
    });

    it('close menu by Escape', () => {
        wrapper = shallow(<CallsNotify calls={messages.calls} tags={messages.tags} users={messages.users}/>);
        wrapper.find('.component').simulate('click');
        expect(wrapper).toMatchSnapshot();
        wrapper.find('.menu').simulate('focus').simulate('keyup', { key: 'Escape' });
        expect(wrapper).toMatchSnapshot();
    });

    it('with classification dialog', () => {
        wrapper = shallow(<CallsNotify calls={messages.calls} tags={messages.tags} users={messages.users}/>);
        wrapper.find('.component').simulate('click');
        const state = [wrapper.state('showClassification')];
        wrapper.find('.classification_btn').simulate('click');
        state.push(wrapper.state('showClassification'));
        expect(state).toEqual([false, true]);
    });
});

describe('LastPerformers', () => {
    const setUp = (user_id = '') => shallow(<LastPerformers user_id={user_id}/>);
    const setUpItem = (el: any, users: any, performers: any) => shallow(<LastPerformerItem el={el}
                                                                                           index={0}
                                                                                           users={users}
                                                                                           performers={performers}/>);
    it('called getData', () => {
        const wrapper = setUp();
        const instance: any = wrapper.instance();
        const spyFunction = jest.spyOn(instance, 'getData');
        instance.componentDidMount();
        expect(spyFunction).toBeCalled();
    });

    it('LastPerformerItem is empty', () => {
        const wrapper = setUpItem({}, {}, {});
        expect(wrapper).toMatchSnapshot();
    });

    it('LastPerformerItem common with categorization', () => {
        const wrapper = setUpItem(performer.el, performer.users, performer.performers);
        expect(wrapper).toMatchSnapshot();
    });

    it('LastPerformerItem common without categorization', () => {
        const perf = deepCopy(performer);
        delete perf.el.categorization;
        const wrapper = setUpItem(perf.el, perf.users, perf.performers);
        expect(wrapper).toMatchSnapshot();
    });

    it('LastPerformerItem duty', () => {
        const perf = deepCopy(performer);
        perf.performers['c3e64a2c-cddd-4390-a79c-bbe17456acd4']?.records?.push({ tag: DUTY_STATE.COMMON });
        const wrapper = setUpItem(perf.el, perf.users, perf.performers);
        expect(wrapper).toMatchSnapshot();
    });
});
