import DeleteDepartmentAlias from './delete-department-alias';

describe('delete_alias_v1', () => {
    it('works', async() => {
        const service = jest.fn(() => ({}));
        const { action } = new DeleteDepartmentAlias();

        const res = await action({ departmentId: 8, alias: 'alias', orgId: 100500 }, { service: () => service } as any);

        expect(res).toEqual({
            alias: 'alias',
            removed: true,
        });
        expect(service).toHaveBeenCalledWith('/v11/departments/8/aliases/alias/', {}, {
            method: 'DELETE',
            orgId: 100500,
        });
    });

    it('validates', async() => {
        const service = jest.fn(() => ({}));
        const { action } = new DeleteDepartmentAlias();

        await expect(action({ alias: '', departmentId: 7, orgId: 100500 }, { service: () => service } as any))
            .rejects.toMatchSnapshot('empty name');
        expect(service).not.toHaveBeenCalled();
    });
});
