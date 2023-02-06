import * as React from 'react';
import { shallow } from 'enzyme';
import { BeruAgeDisclaimerTypeInline } from '../_Type/BeruAgeDisclaimer_type_inline';
import { BeruAgeDisclaimerTypeModal } from '../_Type/BeruAgeDisclaimer_type_modal';
import { IInlineDisclaimerProps, IBaseProps } from '../BeruAgeDisclaimer.types';
import * as utils from '../BeruAgeDisclaimer.utils';

describe('BeruAgeDisclaimer', () => {
    let props: IBaseProps | IInlineDisclaimerProps;
    let setCookie: ReturnType<typeof jest.spyOn>;

    beforeEach(() => {
        props = {
            cookieName: 'test',
            cookieValue: 'testValue',
            rejectUrl: 'https://test.st',
        };
        setCookie = jest.spyOn(utils, 'setCookie');
    });

    describe('тип Inline', () => {
        beforeEach(() => {
            props = {
                ...props as object,
                acceptURL: 'https://test-accept.st',
            } as IInlineDisclaimerProps;
        });

        it('параметр rejectUrl корректно передается в кнопку', () => {
            let button = shallow(<BeruAgeDisclaimerTypeInline {...props as IInlineDisclaimerProps} />).find('BeruButton[theme="normal"]');

            expect(button.prop('url')).toEqual('https://test.st');
        });

        it('клик по кнопке должен устанавливать куку', () => {
            let button = shallow(<BeruAgeDisclaimerTypeInline {...props as IInlineDisclaimerProps} />).find('BeruButton[theme="action"]');

            button.simulate('click');

            expect(setCookie).toHaveBeenCalledWith('test', 'testValue');
        });
    });

    describe('тип Modal', () => {
        it('параметр rejectUrl корректно передается в ссылку', () => {
            let link = shallow(<BeruAgeDisclaimerTypeModal {...props as IBaseProps} />).find('a[target="_top"]');

            expect(link.prop('href')).toEqual('https://test.st');
        });

        it('клик по кнопке должен выставлять куку и скрывать компонент', () => {
            let wrapper = shallow(<BeruAgeDisclaimerTypeModal {...props as IBaseProps} />);
            let button = wrapper.find('BeruButton[theme="action"]');

            button.simulate('click');

            expect(setCookie).toHaveBeenCalledWith('test', 'testValue');
            expect(wrapper.state()).toEqual({ isVisible: false });
        });
    });
});
