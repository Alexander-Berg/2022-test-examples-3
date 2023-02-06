import DeleteGroupMember from './delete-group-member';

describe('delete_group_member_v1', () => {
    it('works', async() => {
        const service = jest.fn(() => ({}));
        const { action } = new DeleteGroupMember();

        const res = await action({ groupId: 8, orgId: 100500, id: 1, type: 'user' }, { service: () => service } as any);

        expect(res).toEqual({
            id: 1,
            type: 'user',
            deleted: true,
        });
        expect(service).toHaveBeenCalledWith('/v11/groups/8/members/bulk-update/', {}, {
            method: 'POST',
            orgId: 100500,
            body: [{
                operation_type: 'remove',
                value: {
                    type: 'user',
                    id: 1,
                },
            }],
        });
    });
});
