import Users from './get-users';

describe('get_users_v1', () => {
    it('works', async() => {
        const service = jest.fn().mockReturnValue({
            result: [{ id: 13 }, { id: 42 }],
            page: 1,
            pages: 5,
            total: 42,
            per_page: 10,
        });
        const { action } = new Users();
        const result = await action({ page: 0 }, { service: () => service } as any);

        expect(service).toHaveBeenCalledWith('/v11/users/', {
            fields: expect.any(String),
            page: 1,
            per_page: 10,
            ordering: 'name',
        }, {});
        expect(result).toEqual({
            users: expect.any(Array),
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
        const { action } = new Users();
        const result = await action({ page: 2, perPage: 3, orgId: 100500 }, { service: () => service } as any);

        expect(service).toHaveBeenCalledWith('/v11/users/', {
            fields: expect.any(String),
            page: 2,
            per_page: 3,
            ordering: 'name',
        }, {
            orgId: 100500,
        });
        expect(result).toEqual({
            users: expect.any(Array),
            page: 2,
            pages: 5,
            perPage: 3,
            total: 14,
        });
    });
});
