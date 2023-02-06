import CreateDepartmentAlias from './create-department-alias';

describe('create_alias_v1', () => {
    it('works', async() => {
        const service = jest.fn(() => ({}));
        const { action } = new CreateDepartmentAlias();

        await action({ alias: 'alias', departmentId: 7, orgId: 100500 }, { service: () => service } as any);
        expect(service).toHaveBeenCalledWith('/v11/departments/7/aliases/', {}, {
            method: 'POST',
            orgId: 100500,
            body: {
                name: 'alias',
            },
        });
    });

    it('validates', async() => {
        const service = jest.fn(() => ({}));
        const { action } = new CreateDepartmentAlias();

        await expect(action({ alias: '', departmentId: 7, orgId: 100500 }, { service: () => service } as any))
            .rejects.toMatchSnapshot('empty name');
        expect(service).not.toHaveBeenCalled();
    });
});
