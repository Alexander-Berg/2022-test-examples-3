import User from './get-user';

describe('get_user_v1', () => {
    it('works', async() => {
        const service = jest.fn().mockReturnValue({ id: 13 });
        const { action } = new User();

        const res = await action({ userId: 8 }, { service: () => service } as any);

        expect(service).toHaveBeenCalledWith('/v11/users/8/', {
            fields: expect.any(String),
        }, {});
        expect(res).toHaveProperty('id', 13);
    });

    it('sends orgId', async() => {
        const service = jest.fn().mockReturnValue({ id: 42 });
        const { action } = new User();

        const res = await action({ userId: 8, orgId: 100500 }, { service: () => service } as any);

        expect(service).toHaveBeenCalledWith('/v11/users/8/', {
            fields: expect.any(String),
        }, {
            orgId: 100500,
        });
        expect(res).toHaveProperty('id', 42);
    });
});
