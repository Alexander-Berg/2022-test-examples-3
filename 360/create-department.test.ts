import CreateDepartment from './create-department';

describe('create_department_v1', () => {
    it('works', async() => {
        const service = jest.fn(() => ({}));
        const { action } = new CreateDepartment();

        await action({ name: 'new name', parentId: 1, orgId: 100500 }, { service: () => service } as any);
        expect(service).toHaveBeenCalledWith('/v11/departments/', {}, {
            method: 'POST',
            orgId: 100500,
            body: {
                name: 'new name',
                parent_id: 1,
                description: undefined,
                external_id: undefined,
                label: undefined,
                head_id: undefined,
            },
        });
    });

    it('validates name', async() => {
        const service = jest.fn(() => ({}));
        const { action } = new CreateDepartment();

        await expect(action({ name: '', parentId: 0 }, { service: () => service } as any))
            .rejects.toMatchSnapshot('no name');
        await expect(action({ name: '1', parentId: 0 }, { service: () => service } as any))
            .rejects.toMatchSnapshot('short name');
        await expect(action({ name: '1'.repeat(41), parentId: 0 }, { service: () => service } as any))
            .rejects.toMatchSnapshot('long name');
        expect(service).not.toHaveBeenCalled();
    });

    it('validates parentId', async() => {
        const service = jest.fn(() => ({}));
        const { action } = new CreateDepartment();

        await expect(action({ name: '12', parentId: 0 }, { service: () => service } as any))
            .rejects.toMatchSnapshot('invalid parent id');
        expect(service).not.toHaveBeenCalled();
    });
});
