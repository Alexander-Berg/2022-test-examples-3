import { shallow } from 'enzyme';
import moment from 'moment';
import React from 'react';

import DatePicker from './index';

describe('DatePicker', () => {

    it('Renders with number value', () => {
        const wrapper: any = shallow(<DatePicker onChange={() => {
        }}
                                                 value={0}/>);
        expect(wrapper.instance()?.state?.value).toEqual(0);
    });

    describe('Format value', () => {
        it('Format valid number value', () => {
            const wrapper: any = shallow(<DatePicker onChange={() => {
            }}
                                                     value={0}/>);
            expect(wrapper.instance()?.state?.value).toEqual(0);
        });

        it('Format Moment.moment value', () => {
            const wrapper: any = shallow(<DatePicker onChange={() => {
            }}
                                                     value={moment(0)}/>);
            expect(wrapper.instance()?.state?.value).toEqual(0);
        });

        it('Format Date value', () => {
            const wrapper: any = shallow(<DatePicker onChange={() => {
            }}
                                                     value={new Date(0)}/>);
            expect(wrapper.instance()?.state?.value).toEqual(0);
        });

        it('Format null value', () => {
            const wrapper: any = shallow(<DatePicker onChange={() => {
            }}
                                                     value={null}/>);
            expect(wrapper.instance()?.state?.value).toEqual('');
        });

    });
});
