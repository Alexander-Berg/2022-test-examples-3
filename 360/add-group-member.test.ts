import AddGroupMember from './add-group-member';

describe('add_group_member_v1', () => {
    it('works', async() => {
        const service = jest.fn(() => ({}));
        const { action } = new AddGroupMember();

        const res = await action({ groupId: 8, orgId: 100500, id: 1, type: 'user' }, { service: () => service } as any);

        expect(res).toEqual({
            id: 1,
            type: 'user',
            added: true,
        });
        expect(service).toHaveBeenCalledWith('/v11/groups/8/members/', {}, {
            method: 'POST',
            orgId: 100500,
            body: {
                type: 'user',
                id: 1,
            },
        });
    });
});
