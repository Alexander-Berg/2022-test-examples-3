import { transform, transformShort, transformMembers, addOrgId, RawGroup } from './common';

describe('common', () => {
    it('transform works', () => {
        const rawRes: RawGroup = {
            id: 13,
            name: 'name',
            type: 'type',
            description: 'x',
            members_count: 42,
            label: 'l',
            email: 'l@example.com',
            aliases: ['al'],
            external_id: 'ex',
            removed: false,
            members: [
                { type: 'user', object: { id: 1 } },
                { type: 'group', object: { id: 2 } },
                { type: 'department', object: { id: 3 } },
            ],
            admins: [
                { id: 1 },
            ],
            author_id: 2,
            uid: 10,
            member_of: [{ id: 1 }],
            created: '2022-03-25T07:48:32.536594Z',
        };
        const result = transform(rawRes);

        expect(result).toMatchSnapshot();
    });

    it('transform default works', () => {
        const rawRes: RawGroup = {
            id: 13,
            name: 'name',
            type: 'type',
            description: null,
            members_count: 42,
            label: null,
            email: null,
            aliases: [],
            external_id: null,
            removed: false,
            members: [
                { type: 'user', object: { id: 1 } },
            ],
            admins: [],
            author_id: 2,
            uid: null,
            member_of: [],
            created: '2022-03-25T07:48:32.536594Z',
        };
        const result = transform(rawRes);

        expect(result).toMatchSnapshot();
    });

    it('transformShort works', () => {
        const rawRes: Partial<RawGroup> = {
            id: 13,
            name: 'name',
            members_count: 42,
        };

        const result = transformShort(rawRes);

        expect(result).toMatchSnapshot();
    });

    it('transformMembers works', () => {
        const result = transformMembers([
            { type: 'user', object: { id: 1 } },
            { type: 'group', object: { id: 2 } },
            { type: 'department', object: { id: 3 } },
        ]);

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
