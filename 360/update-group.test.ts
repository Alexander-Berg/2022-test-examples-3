import UpdateGroup from './update-group';
import type { Params } from './update-group';

describe('update_group_v1', () => {
    it('works', async() => {
        const service = jest.fn(() => ({ members: [], admins: [], member_of: [] }));
        const { action } = new UpdateGroup();

        await action({
            groupId: 8,
            orgId: 100500,
            name: 'new',
            adminIds: [1],
        }, { service: () => service } as any);

        expect(service).toHaveBeenCalledWith('/v11/groups/8/', {}, {
            method: 'PATCH',
            orgId: 100500,
            body: {
                name: 'new',
                admins: [{ id: 1 }],
            },
        });
    });

    const fn = (params: Params) => {
        try {
            UpdateGroup.normalize(params);
        } catch (e) {
            return e;
        }
    };

    it('pass valid params', () => {
        expect(UpdateGroup.normalize({ groupId: 1 })).toEqual({ groupId: 1 });
    });

    it('converts externalId', () => {
        expect(UpdateGroup.normalize({ groupId: 1, externalId: '' })).toEqual({
            groupId: 1,
            externalId: null,
        });
    });

    it('validate groupId', () => {
        expect(fn({ groupId: 0 })).toMatchObject({ code: 'invalid_group_id' });
    });

    it('validate name', () => {
        expect(fn({ groupId: 1, name: '' })).toMatchObject({ code: 'invalid_name' });
    });
});
