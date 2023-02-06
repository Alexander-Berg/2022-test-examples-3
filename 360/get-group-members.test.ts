import GetGroupMembers from './get-group-members';

describe('get_group_members_v1', () => {
    it('works', async() => {
        const service = jest.fn(() => [
            { type: 'user', object: { id: 1 } },
            { type: 'group', object: { id: 1 } },
            { type: 'department', object: { id: 1 } },
        ]);
        const { action } = new GetGroupMembers();

        await action({
            orgId: 100500,
            groupId: 8,
        }, { service: () => service } as any);

        expect(service).toHaveBeenCalledWith('/v11/groups/8/members/', {}, {
            orgId: 100500,
        });
    });
});
