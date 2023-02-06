import * as React from 'react';
import { shallow } from 'enzyme';
import b from '@yandex-turbo/core/cn';
import { Hash } from '@yandex-turbo/components/lcTypes/lcTypes';

import { LcAlign, LcFont, LcSizePx, LcSizes, LcTypeface } from '@yandex-turbo/components/lcTypes/lcTypes';
import { LcButton } from '@yandex-turbo/components/LcButton/LcButton';
import { LcButtonThemes } from '@yandex-turbo/components/LcButton/LcButton.types';
import { LcTextBlock } from '@yandex-turbo/components/LcTextBlock/LcTextBlock';
import { LcEmailSubscriptionComponent as LcEmailSubscription } from '../LcEmailSubscription';
import { LcEmailSubscriptionProps } from '../LcEmailSubscription.types';

const cls = b('lc-email-subscription');
const formSelector = `.${cls('subscription-form')()}`;

const responseMessage = { status: 'success' };
const mockFetch = Promise.resolve({
    status: 200,
    json: () => Promise.resolve(responseMessage),
});

// eslint-disable-next-line @typescript-eslint/no-explicit-any
(global as Hash<any>).fetch = jest.fn().mockImplementation(() => mockFetch);

describe('LcEmailSubscription', () => {
    let defaultProps: LcEmailSubscriptionProps;

    beforeEach(() => {
        defaultProps = {
            title: {
                content: 'Подпишитесь на новости',
                size: LcSizePx.s36,
                font: LcFont.DISPLAY,
                typeface: LcTypeface.MEDIUM,
            },
            titleOffsets: {
                top: LcSizes.NONE,
                bottom: LcSizes.S,
            },
            align: LcAlign.LEFT,
            inputPlaceholder: 'Электронная почта',
            emailSubscriptionPath: '',
            mailList: '',
            account: '',
            successMessage: 'Готово! Осталось подтвердить адрес, кликнув по кнопке в присланном письме.',
            errorMessage: 'Ошибка сервера, не удалось подписаться.',
            size: LcSizes.M,
            button: {
                label: 'Подписаться',
                theme: LcButtonThemes.Action,
            },
            email: '',
            events: [],
        };
    });

    it('should render LcEmailSubscription', () => {
        const component = shallow(<LcEmailSubscription {...defaultProps} />);

        expect(component.isEmptyRender()).toBe(false);
    });

    it('should contain email in state, if it is passed from context', () => {
        const props = { ...defaultProps, email: 'saaaaaaaaasha@yandex-team.ru' };
        const component = shallow<LcEmailSubscription>(<LcEmailSubscription {...props} />);

        expect(component.instance().state.email).toBe('saaaaaaaaasha@yandex-team.ru');
    });

    it('should hide form on success', () => {
        const component = shallow(<LcEmailSubscription {...defaultProps} />);

        expect(component.find(formSelector)).toHaveLength(1);
        component.setState({ success: true });
        expect(component.find(formSelector)).toHaveLength(0);
    });

    it('should render success message after success', () => {
        const props = { ...defaultProps };
        props.title.content = '';

        const component = shallow(<LcEmailSubscription {...props} />);

        expect(component.find(LcTextBlock)).toHaveLength(0);
        component.setState({ success: true });

        const success = component.find(LcTextBlock);

        expect(success.prop('content')).toBe(props.successMessage);
    });

    it('should render error message after error', () => {
        const props = { ...defaultProps };
        props.title.content = '';

        const component = shallow(<LcEmailSubscription {...props} />);

        expect(component.find(LcTextBlock)).toHaveLength(0);
        component.setState({ error: true });

        const error = component.find(LcTextBlock);

        expect(error.prop('content')).toBe(props.errorMessage);
    });

    it('should change state on successful subscription', done => {
        responseMessage.status = 'success';

        const props = { ...defaultProps };
        props.title.content = '';

        const component = shallow(<LcEmailSubscription {...props} />);
        const email = 'saaaaaaaaasha@yandex-team.ru';

        component.setState({ email });

        component.find(LcButton).simulate('click', {
            stopPropagation: () => {},
            preventDefault: () => {},
        });

        process.nextTick(() => {
            expect(component.state()).toEqual({
                email,
                success: true,
                isLoading: false,
                error: false,
            });

            done();
        });
    });

    it('should change state on error during subscription', done => {
        responseMessage.status = 'fail';

        const props = { ...defaultProps };
        props.title.content = '';

        const component = shallow(<LcEmailSubscription {...props} />);
        const email = 'saaaaaaaaasha@yandex-team.ru';

        component.setState({ email });

        component.find(LcButton).simulate('click', {
            stopPropagation: () => {},
            preventDefault: () => {},
        });

        process.nextTick(() => {
            expect(component.state()).toEqual({
                email,
                success: false,
                error: true,
                isLoading: false,
            });

            done();
        });
    });
});
