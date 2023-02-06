import Department from './get-department';

describe('get_department_v1', () => {
    it('works', async() => {
        const service = jest.fn().mockReturnValue({ id: 13 });
        const { action } = new Department();

        const res = await action({ departmentId: 8 }, { service: () => service } as any);

        expect(service).toHaveBeenCalledWith('/v11/departments/8/', {
            fields: expect.any(String),
        }, {});
        expect(res).toHaveProperty('id', 13);
    });

    it('sends orgId', async() => {
        const service = jest.fn().mockReturnValue({ id: 42, email: 'cover@me' });
        const { action } = new Department();

        const res = await action({ departmentId: 8, orgId: 100500 }, { service: () => service } as any);

        expect(service).toHaveBeenCalledWith('/v11/departments/8/', {
            fields: expect.any(String),
        }, {
            orgId: 100500,
        });
        expect(res).toHaveProperty('id', 42);
    });

    it('validates', async() => {
        const service = jest.fn(() => ({}));
        const { action } = new Department();

        await expect(action({ departmentId: 0 }, { service: () => service } as any))
            .rejects.toMatchSnapshot('invalid department id');
        expect(service).not.toHaveBeenCalled();
    });
});
