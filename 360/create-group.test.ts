import CreateGroup from './create-group';
import type { Params } from './create-group';

describe('create_group_v1', () => {
    it('works', async() => {
        const service = jest.fn(() => ({ members: [], admins: [], member_of: [] }));
        const { action } = new CreateGroup();

        await action({
            orgId: 100500,
            name: 'n',
            description: 'd',
            adminIds: [1],
            label: 'l',
            members: [{
                type: 'user',
                id: 1,
            }],
        }, { service: () => service } as any);

        expect(service).toHaveBeenCalledWith('/v11/groups/', {}, {
            method: 'POST',
            orgId: 100500,
            body: {
                name: 'n',
                description: 'd',
                admins: [{ id: 1 }],
                label: 'l',
                members: [{
                    type: 'user',
                    id: 1,
                }],
            },
        });
    });

    const fn = (params: Params) => {
        try {
            CreateGroup.normalize(params);
        } catch (e) {
            return e;
        }
    };

    it('pass valid params', () => {
        expect(CreateGroup.normalize({ name: 'n' })).toEqual({ name: 'n' });
    });

    it('validate name', () => {
        expect(fn({ name: '' })).toMatchObject({ code: 'invalid_name' });
    });
});
