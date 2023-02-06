import UpdateDepartment from './update-department';

describe('update_department_v1', () => {
    it('works', async() => {
        const service = jest.fn(() => ({}));
        const { action } = new UpdateDepartment();

        await action({ departmentId: 8, name: 'new name' }, { service: () => service } as any);
        expect(service).toHaveBeenCalledWith('/v11/departments/8/', {}, {
            method: 'PATCH',
            body: {
                name: 'new name',
                parent_id: undefined,
                description: undefined,
                external_id: undefined,
                label: undefined,
                head_id: undefined,
            },
        });
    });

    it('sends orgId', async() => {
        const service = jest.fn(() => ({}));
        const { action } = new UpdateDepartment();

        await action({
            departmentId: 8,
            orgId: 100500,
            parentId: 2,
            description: 'd',
            externalId: 'e',
            label: 'l',
            headId: 13,

        }, { service: () => service } as any);
        expect(service).toHaveBeenCalledWith('/v11/departments/8/', {}, {
            method: 'PATCH',
            orgId: 100500,
            body: {
                parent_id: 2,
                description: 'd',
                external_id: 'e',
                label: 'l',
                head_id: 13,
            },
        });
    });

    it('validates departmentId', async() => {
        const service = jest.fn(() => ({}));
        const { action } = new UpdateDepartment();

        await expect(action({ departmentId: 0 }, { service: () => service } as any))
            .rejects.toMatchSnapshot('no deparmentId');
        expect(service).not.toHaveBeenCalled();
    });

    it('validates name', async() => {
        const service = jest.fn(() => ({}));
        const { action } = new UpdateDepartment();

        await expect(action({ departmentId: 8, name: '1' }, { service: () => service } as any))
            .rejects.toMatchSnapshot('short name');
        await expect(action({ departmentId: 8, name: '1'.repeat(41) }, { service: () => service } as any))
            .rejects.toMatchSnapshot('long name');
        expect(service).not.toHaveBeenCalled();
    });

    it('validates parentId', async() => {
        const service = jest.fn(() => ({}));
        const { action } = new UpdateDepartment();

        await expect(action({ departmentId: 8, parentId: 0 }, { service: () => service } as any))
            .rejects.toMatchSnapshot('invalid parentId');
        expect(service).not.toHaveBeenCalled();
    });
});
