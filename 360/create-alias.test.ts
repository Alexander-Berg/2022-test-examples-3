import CreateAlias from './create-alias';

describe('create_alias_v1', () => {
    it('works', async() => {
        const service = jest.fn(() => ({}));
        const { action } = new CreateAlias();

        await action({ alias: 'alias', userId: 7, orgId: 100500 }, { service: () => service } as any);
        expect(service).toHaveBeenCalledWith('/v11/users/7/aliases/', {}, {
            method: 'POST',
            orgId: 100500,
            body: {
                name: 'alias',
            },
        });
    });
});
