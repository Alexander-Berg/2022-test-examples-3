import { transform, transformShort, addOrgId, RawUser } from './common';

describe('common', () => {
    function prepare(): RawUser {
        const raw: RawUser = {
            id: 42,
            nickname: 'nickname',
            department_id: 42,
            email: 'email@example.com',
            name: {
                first: 'Firstname',
                last: 'Lastname',
            },
            gender: 'male',
            position: 'position',
            about: 'about',
            avatar_id: 'avatar_id',
            birthday: '2022-02-24',
            contacts: [
                {
                    type: 'type',
                    value: 'value',
                    main: true,
                    alias: false,
                    synthetic: false,
                },
            ],
            aliases: ['alias'],
            groups: [{ id: 42 }],
            external_id: 'ext',
            is_admin: false,
            is_robot: false,
            is_dismissed: false,
            is_enabled: false,
            timezone: 'Europe/Moscow',
            language: 'ru',
            created: '2019-12-25T13:18:58.765173Z',
            updated_at: '2019-12-25T13:18:58.765173Z',
        };

        return raw;
    }

    it('transform works', () => {
        const result = transform(prepare());

        expect(result).toMatchSnapshot();
    });

    it('transform null works', () => {
        const raw = prepare();

        raw.aliases.length = 0;
        raw.contacts.length = 0;
        raw.groups.length = 0;
        raw.email = null;
        raw.gender = null;
        raw.position = null;
        raw.about = null;
        raw.avatar_id = null;
        raw.birthday = null;
        raw.external_id = null;
        delete raw.created;
        delete raw.updated_at;
        delete raw.department_id;
        raw.department = { id: 42 };

        const result = transform(raw);

        expect(result).toMatchSnapshot({
            createdAt: expect.any(String),
            updatedAt: expect.any(String),
        });
    });

    it('transformShort works', () => {
        const result = transformShort(prepare());

        expect(result).toMatchSnapshot();
    });

    it('addOrgId works', () => {
        const options = {};

        addOrgId({ orgId: 1 }, options);
        expect(options).toHaveProperty('orgId', 1);
    });

    it('addOrgId does not modify options', () => {
        const options = Object.freeze({ addUidHeader: false });
        const res = addOrgId({}, options);

        expect(options).not.toHaveProperty('orgId');
        expect(res).toBe(options);
    });
});
