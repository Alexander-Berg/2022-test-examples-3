import GetGroup from './get-group';

describe('get_group_v1', () => {
    it('works', async() => {
        const service = jest.fn(() => ({ members: [], admins: [], member_of: [] }));
        const { action } = new GetGroup();

        await action({
            orgId: 100500,
            groupId: 8,
        }, { service: () => service } as any);

        expect(service).toHaveBeenCalledWith('/v11/groups/8/', {
            fields: expect.any(String),
        }, {
            orgId: 100500,
        });
    });
});
