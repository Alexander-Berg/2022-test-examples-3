import * as React from 'react';
import { mount } from 'enzyme';
import { LcButton } from '@yandex-turbo/components/LcButton/LcButton';
import { LcButtonActions } from '@yandex-turbo/components/LcButton/LcButton.types';
import { ILcPassport } from '@yandex-turbo/components/lcTypes/lcTypes';
import { LcFormBase as LcForm } from '../LcForm';

const mockedFunc = jest.fn();

jest.mock('@yandex-turbo/core/goals', () => {
    return {
        reachYaMetrikaGoals: () => mockedFunc,
    };
});

describe('Goals', () => {
    const pageId = 555;
    const goals = [
        {
            name: 'goal 1',
            id: 1,
        },
        {
            name: 'goal 2',
            id: 2,
        },
    ];

    const children = [<LcButton actionType={LcButtonActions.SUBMIT} key="1" />];

    const modalContent = {
        goals,
    };

    const passport: ILcPassport = {
        login: 'test',
        account_info: {
            is_beta_tester: false,
            is_yandexoid: false,
        },
        logged_in: 1,
        session_id: 'session id',
        environment: 'env',
        session_status: 123,
        id: 'id',
        cookieL: null,
    };

    it('reachGoal invoking upon success', () => {
        const component = mount(
            <LcForm
                children={children}
                modalContent={modalContent}
                action={undefined}
                defaultValues={{}}
                lang={{}}
                meta=""
                name="lc-form-test"
                passport={passport}
                sk="sk"
                pageId={pageId}
                goals={{
                    autocompleted: { name: 'foo' },
                    autocompleteSuccess: { name: 'bar' },
                }}
            />
        );

        component.find('.lc-button_type_submit').simulate('click');

        setTimeout(() => {
            expect(mockedFunc).toHaveBeenCalledWith({
                goals,
                pageId,
            });
        }, 100);
    });

    it('reachGoal does not invoking upon fail', () => {
        const component = mount(
            <LcForm
                children={children}
                modalContent={modalContent}
                action="fail action"
                defaultValues={{}}
                lang={{}}
                meta=""
                name="lc-form-test"
                passport={passport}
                sk="sk"
                pageId={pageId}
                goals={{
                    autocompleted: { name: 'foo' },
                    autocompleteSuccess: { name: 'bar' },
                }}
            />
        );

        component.find('.lc-button_type_submit').simulate('click');

        setTimeout(() => {
            expect(mockedFunc).not.toHaveBeenCalled();
        }, 100);
    });
});
