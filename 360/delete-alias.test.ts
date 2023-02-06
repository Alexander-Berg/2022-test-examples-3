import DeleteAlias from './delete-alias';

describe('delete_alias_v1', () => {
    it('works', async() => {
        const service = jest.fn(() => ({}));
        const { action } = new DeleteAlias();

        const res = await action({ userId: 8, alias: 'alias', orgId: 100500 }, { service: () => service } as any);

        expect(res).toEqual({
            alias: 'alias',
            removed: true,
        });
        expect(service).toHaveBeenCalledWith('/v11/users/8/aliases/alias/', {}, {
            method: 'DELETE',
            orgId: 100500,
        });
    });
});
