import { shallow } from 'enzyme';
import React from 'react';

import { REQUEST_TYPES } from '../../constants';
import { RequestTypeControlMini } from './RequestTypeControl';

const MAX_ICONS_AMOUNT = 3;
const TITLE = 'title';
const setUp = (props: {typesArr: REQUEST_TYPES[]}) => shallow(
    <RequestTypeControlMini {...props}>{TITLE}</RequestTypeControlMini>,
);

describe('RequestTypeControlMini', () => {
    it('Should work with empty props', () => {
        const MOCK_PROPS: any = {};
        const component = setUp(MOCK_PROPS);
        expect(component).toMatchSnapshot();
    });

    it('Should work with 1 element', () => {
        const component = setUp({ typesArr: [REQUEST_TYPES.smm] });
        expect(component).toMatchSnapshot();
    });

    it('Should render maximum than 3 icons', () => {
        const typesArr = [
            REQUEST_TYPES.smm,
            REQUEST_TYPES.email,
            REQUEST_TYPES.dispatcher,
            REQUEST_TYPES.incoming,
            REQUEST_TYPES.chat,
        ];
        const component = setUp({ typesArr });

        expect(component.find('.icon_multiple').length).toEqual(MAX_ICONS_AMOUNT);
    });

    it('Should not render icon if string doesn\'t exist in REQUEST_TYPES enum', () => {
        const NON_VALID_PROPS: any = ['element'];
        const component = setUp({ typesArr: NON_VALID_PROPS });
        expect(component).toMatchSnapshot();
    });
});
