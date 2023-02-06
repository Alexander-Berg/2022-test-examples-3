import UpdateUser from './update-user';
import type { Params } from './update-user';

describe('update_user_v1', () => {
    it('works', async() => {
        const service = jest.fn(() => ({
            department_id: 13,
            name: {
                first: 'f',
                last: 'l',
            },
            created: '2022-03-23T19:57:37.745Z',
            updated_at: '2022-03-23T19:57:37.745Z',
        }));
        const { action } = new UpdateUser();

        await action({ userId: 8, position: 'pos' }, { service: () => service } as any);
        expect(service).toHaveBeenCalledWith('/v11/users/8/', {
            fields: expect.any(String),
        }, {
            method: 'PATCH',
            body: {
                about: undefined,
                birthday: undefined,
                contacts: undefined,
                department_id: undefined,
                external_email: undefined,
                external_id: undefined,
                gender: undefined,
                groups: undefined,
                is_admin: undefined,
                is_dismissed: undefined,
                is_enabled: undefined,
                language: undefined,
                login: undefined,
                name: undefined,
                password: undefined,
                password_change_required: undefined,
                position: 'pos',
                timezone: undefined,
            },
        });
    });

    it('sends orgId', async() => {
        const service = jest.fn(() => ({
            department_id: 13,
            name: {
                first: 'f',
                last: 'l',
            },
            created: '2022-03-23T19:57:37.745Z',
            updated_at: '2022-03-23T19:57:37.745Z',
        }));
        const { action } = new UpdateUser();

        await action({
            userId: 8,
            orgId: 100500,
            about: 'a',
            position: 'p',
            gender: '',
            departmentId: 13,

        }, { service: () => service } as any);
        expect(service).toHaveBeenCalledWith('/v11/users/8/', {
            fields: expect.any(String),
        }, {
            method: 'PATCH',
            orgId: 100500,
            body: {
                about: 'a',
                birthday: undefined,
                contacts: undefined,
                department_id: 13,
                external_id: undefined,
                gender: null,
                is_admin: undefined,
                is_dismissed: undefined,
                is_enabled: undefined,
                language: undefined,
                name: undefined,
                password: undefined,
                password_change_required: undefined,
                position: 'p',
                timezone: undefined,
            },
        });
    });

    const fn = (params: Params) => {
        try {
            UpdateUser.normalize(params);
        } catch (e) {
            return e;
        }
    };

    it('pass valid params', () => {
        expect(UpdateUser.normalize({ userId: 1 })).toEqual({ userId: 1 });
    });

    it('converts externalId', () => {
        expect(UpdateUser.normalize({ userId: 1, externalId: '' })).toEqual({
            userId: 1,
            externalId: null,
        });
    });

    it('validate userId', () => {
        expect(fn({ userId: 0 })).toMatchObject({ code: 'invalid_user_id' });
    });

    it('validate password', () => {
        expect(fn({ userId: 1, passwordChangeRequired: true })).toMatchObject({ code: 'password_required' });
    });
});
