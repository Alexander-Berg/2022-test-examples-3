import GetGroups from './get-groups';

describe('get_groups_v1', () => {
    it('works', async() => {
        const service = jest.fn(() => ({
            result: [{ members: [], admins: [], member_of: [] }],
        }));
        const { action } = new GetGroups();

        await action({
            orgId: 100500,
            page: 2,
        }, { service: () => service } as any);

        expect(service).toHaveBeenCalledWith('/v11/groups/', {
            fields: expect.any(String),
            page: 2,
            per_page: 10,
        }, {
            orgId: 100500,
        });
    });
});
