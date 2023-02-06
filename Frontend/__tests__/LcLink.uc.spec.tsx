import * as React from 'react';
import { shallow } from 'enzyme';
import { LcLinkUcComponent as LcLinkUc } from '../LcLink.uc';

describe('<LcLinkUc/> component', () => {
    describe('notModifyTarget prop', () => {
        it('Should correctly set notModifyTarget when it is passed', () => {
            const link = shallow(<LcLinkUc url="https://ya.ru" notModifyTarget />);
            expect(link.prop('notModifyTarget')).toEqual(true);
        });

        it('Should correctly set notModifyTarget when it is not passed', () => {
            const link = shallow(<LcLinkUc url="https://ya.ru" />);
            expect(link.prop('notModifyTarget')).toEqual(undefined);
        });

        it('Should correctly set notModifyTarget when prop isSiteWithSpecialHostName is passed', () => {
            const link = shallow(<LcLinkUc url="https://ya.ru" isSiteWithSpecialHostName />);
            expect(link.prop('notModifyTarget')).toEqual(true);
        });

        it('Should correctly set notModifyTarget when prop isSiteWithSpecialHostName is passed as false', () => {
            const link = shallow(<LcLinkUc url="https://ya.ru" isSiteWithSpecialHostName={false} />);
            expect(link.prop('notModifyTarget')).toEqual(false);
        });

        it('Should correctly set notModifyTarget when props notModifyTarget and isSiteWithSpecialHostName are passed', () => {
            const link = shallow(<LcLinkUc url="https://ya.ru" isSiteWithSpecialHostName notModifyTarget={false} />);
            expect(link.prop('notModifyTarget')).toEqual(false);
        });
    });
});
