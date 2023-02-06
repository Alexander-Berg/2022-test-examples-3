import { transform, transformShort, addOrgId, RawDepartment } from './common';

describe('common', () => {
    function prepare(): RawDepartment {
        return {
            id: 13,
            name: 'name',
            parent_id: 1,
            description: 'x',
            created: '2019-12-25T13:18:58.765173Z',
            external_id: 'ext',
            label: 'l',
            email: null,
            head: { id: 123456 },
            members_count: 42,
            aliases: [],
            unknown: 1,
            uid: 42,
        } as RawDepartment;
    }

    it('transform works', () => {
        const result = transform(prepare());

        expect(result).toMatchSnapshot();
    });

    it('transform get parentId from parent', () => {
        const raw = prepare();

        raw.parent = { id: 13 };
        delete raw.parent_id;

        const result = transform(raw);

        expect(result).toMatchSnapshot();
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
