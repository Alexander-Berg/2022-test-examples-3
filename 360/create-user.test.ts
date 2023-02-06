import CreateUser from './create-user';
import type { Params } from './create-user';

describe('create_user_v1', () => {
    it('works', async() => {
        const service = jest.fn(() => ({
            department_id: 13,
            name: {
                first: 'f',
                last: 'l',
            },
        }));
        const { action } = new CreateUser();

        await action({
            nickname: 'n',
            about: 'a',
            departmentId: 13,
            name: {
                first: 'f',
                last: 'l',
            },
            password: 'p',
            orgId: 100500,
        }, { service: () => service } as any);
        expect(service).toHaveBeenCalledWith('/v11/users/', {}, {
            method: 'POST',
            orgId: 100500,
            body: {
                nickname: 'n',
                department_id: 13,
                name: {
                    first: 'f',
                    last: 'l',
                },
                gender: undefined,
                position: undefined,
                about: 'a',
                birthday: undefined,
                contacts: undefined,
                external_id: undefined,
                is_admin: undefined,
                is_enabled: undefined,
                timezone: undefined,
                language: undefined,
                password: 'p',
            },
        });
    });

    const p: Params = Object.freeze({
        nickname: 'n',
        departmentId: 13,
        password: 'p',
        name: {
            first: 'f',
            last: 'l',
        },
    });

    const fn = (params: Params) => {
        try {
            CreateUser.normalize(params);
        } catch (e) {
            return e;
        }
    };

    it('pass valid params', () => {
        expect(CreateUser.normalize(p)).toEqual(p);
    });

    it('validate nickname', () => {
        const params = { ...p };

        params.nickname = '';
        expect(fn(params)).toMatchObject({ code: 'invalid_nickname' });
    });

    it('validate departmentId', () => {
        const params = { ...p };

        params.departmentId = 0;
        expect(fn(params)).toMatchObject({ code: 'invalid_department_id' });
    });

    it('validate password', () => {
        const params = { ...p };

        params.password = undefined;
        expect(fn(params)).toMatchObject({ code: 'password_required' });
    });

    it('validate name', () => {
        const params = { ...p };

        params.name = undefined;
        expect(fn(params)).toMatchObject({ code: 'invalid_name' });
    });
});
