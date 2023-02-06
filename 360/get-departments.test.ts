import Departments from './get-departments';

describe('get_departments_v1', () => {
    it('works', async() => {
        const service = jest.fn().mockReturnValue({
            result: [{ id: 13 }, { id: 42 }],
            page: 1,
            pages: 5,
            per_page: 10,
            total: 42,
        });
        const { action } = new Departments();
        const result = await action({ page: 0 }, { service: () => service } as any);

        expect(service).toHaveBeenCalledWith('/v11/departments/', {
            fields: expect.any(String),
            page: 1,
            per_page: 10,
            ordering: 'id',
        }, {});
        expect(result).toEqual({
            departments: expect.any(Array),
            page: 1,
            pages: 5,
            perPage: 10,
            total: 42,
        });
    });

    it('sends params', async() => {
        const service = jest.fn().mockReturnValue({
            result: [{ id: 13 }, { id: 42 }, { id: 24 }],
            page: 2,
            pages: 5,
            per_page: 3,
            total: 14,
        });
        const { action } = new Departments();
        const result = await action({ page: 2, perPage: 3, orgId: 100500, orderBy: 'name', parentId: 8 }, { service: () => service } as any);

        expect(service).toHaveBeenCalledWith('/v11/departments/', {
            fields: expect.any(String),
            page: 2,
            per_page: 3,
            parent_id: 8,
            ordering: 'name',
        }, {
            orgId: 100500,
        });
        expect(result).toEqual({
            departments: expect.any(Array),
            page: 2,
            pages: 5,
            perPage: 3,
            total: 14,
        });
    });

    it('validates', async() => {
        const service = jest.fn(() => ({}));
        const { action } = new Departments();

        await expect(action({ perPage: -1 }, { service: () => service } as any))
            .rejects.toMatchSnapshot('negative perPage');
        await expect(action({ perPage: 100500 }, { service: () => service } as any))
            .rejects.toMatchSnapshot('too big perPage');
        expect(service).not.toHaveBeenCalled();
    });
});
