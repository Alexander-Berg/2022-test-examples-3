import * as React from 'react';
import { shallow } from 'enzyme';
import { GlobalActions } from '@yandex-turbo/core/state/global/reducer';
import { AuthComponent, IProps } from '../Auth';
import { Auth as AuthPhone } from '../Auth@phone';
import { Auth as AuthDesktop } from '../Auth@desktop';
import { AuthorizationStatus } from '../AuthApi';

const authProps: IProps = {
    authEndpoint: 'http://localhost:3333/auth?action=auth&result=error',
    loginEndpoint: 'http://localhost:3333/auth?action=login',
    logoutEndpoint: 'http://localhost:3333/auth?action=logout',
    yandexLoginEndpoint: 'http://localhost:3333/auth?action=yandex-login',
};

jest.mock('@yandex-turbo/core/ajax', () => ({
    get: jest.fn(),
}));

describe('Auth component', () => {
    [[AuthPhone, 'phone'], [AuthDesktop, 'desktop']].forEach(([Auth, platform]) => {
        describe(platform as string, () => {
            it('should render without crashing', () => {
                const authComponent = shallow(
                    <Auth {...authProps} />
                );

                expect(authComponent.length).toEqual(1);
            });

            it('should dispatch loginFormVisible on button click', () => {
                let correctType = false;
                let correctLoginFormVisible = false;

                const mock = jest.mock('../Auth', () => ({
                    authState: jest.fn(() => ({ status: AuthorizationStatus.loggedOut })),
                }));

                const dispatch = jest.fn(action => {
                    correctType = action.type === GlobalActions.UPDATE;
                    correctLoginFormVisible = action.payload.loginFormVisible === true;
                });

                const state = { auth: { status: AuthorizationStatus.loggedOut } };

                const authComponent = shallow(
                    // @ts-ignore
                    <AuthComponent {...authProps} state={state} dispatch={dispatch} />
                );

                authComponent.find('.turbo-auth__button').simulate('click');

                expect(correctType).toEqual(true);
                expect(correctLoginFormVisible).toEqual(true);

                mock.resetAllMocks();
                dispatch.mockReset();
            });
        });
    });
});
