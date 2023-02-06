import DeleteGroup from './delete-group';

describe('delete_group_v1', () => {
    it('works', async() => {
        const service = jest.fn(() => ({}));
        const { action } = new DeleteGroup();

        const res = await action({ groupId: 8, orgId: 100500 }, { service: () => service } as any);

        expect(res).toEqual({
            id: 8,
            removed: true,
        });
        expect(service).toHaveBeenCalledWith('/v11/groups/8/', {}, {
            method: 'DELETE',
            orgId: 100500,
        });
    });

    it('validates', async() => {
        const service = jest.fn(() => ({}));
        const { action } = new DeleteGroup();

        await expect(action({ groupId: 0 }, { service: () => service } as any))
            .rejects.toMatchSnapshot('invalid group id');
        expect(service).not.toHaveBeenCalled();
    });
});
