import DeleteDepartment from './delete-department';

describe('delete_department_v1', () => {
    it('works', async() => {
        const service = jest.fn(() => ({}));
        const { action } = new DeleteDepartment();

        const res = await action({ departmentId: 8, orgId: 100500 }, { service: () => service } as any);

        expect(res).toEqual({
            id: 8,
            removed: true,
        });
        expect(service).toHaveBeenCalledWith('/v11/departments/8/', {}, {
            method: 'DELETE',
            orgId: 100500,
        });
    });

    it('validates', async() => {
        const service = jest.fn(() => ({}));
        const { action } = new DeleteDepartment();

        await expect(action({ departmentId: 0 }, { service: () => service } as any))
            .rejects.toMatchSnapshot('invalid department id');
        expect(service).not.toHaveBeenCalled();
    });
});
