describe('utils/scopes', function() {
    const createScopes = require('./scopes');

    it('createScopes', function() {
        var expected = [
            {
                type: 'role',
                data: {
                    important: 0,
                    sparsed: 0,
                    blocked: 0
                },
                size: 2,
                currentSize: 2,
                members: [
                    {
                        person: { id: 'person-b' },
                        role: {
                            code: 'product_head',
                            scope: []
                        },
                        state: 'state-a'
                    },
                    {
                        person: { id: 'person-a' },
                        role: {
                            code: 'role-a',
                            scope: []
                        },
                        state: 'state-a'
                    }
                ]
            },
            {
                type: 'role',
                data: {
                    id: 'unapproved',
                    important: 1,
                    sparsed: 0,
                    blocked: 0
                },
                size: 1,
                currentSize: 1,
                members: [
                    {
                        person: { id: 'person-b' },
                        role: {
                            code: 'role-b',
                            scope: ['scope-a', 'scope-b']
                        },
                        state: 'waiting_approval'
                    }
                ]
            }
        ];

        var actual = createScopes([
            {
                person: { id: 'person-a' },
                role: {
                    code: 'role-a',
                    scope: []
                },
                state: 'state-a'
            },
            {
                person: { id: 'person-b' },
                role: {
                    code: 'product_head',
                    scope: []
                },
                state: 'state-a'
            },
            {
                person: { id: 'person-b' },
                role: {
                    code: 'role-b',
                    scope: ['scope-a', 'scope-b']
                },
                state: 'waiting_approval'
            }
        ], 'role');

        expect(actual).toEqual(expected);
    });

    it('createScopes dept', function() {
        var expected = [
            {
                type: 'dept',
                data: {
                    blocked: 0,
                    sparsed: 1
                },
                size: 1,
                currentSize: 1,
                members: [
                    {
                        person: { id: 'person-b' },
                        role: { code: 'product_head' },
                        state: 'state-a'
                    }
                ]
            },
            {
                type: 'dept',
                data: {
                    blocked: 0,
                    sparsed: 0,
                    name: 'value',
                    serviceMemberDepartmentId: 1,
                    id: 1
                },
                size: 2,
                currentSize: 2,
                members: [
                    {
                        person: { id: 'person-a' },
                        role: { code: 'role-a' },
                        state: 'state-a',
                        fromDepartment: { name: 'value' },
                        serviceMemberDepartmentId: 1
                    },
                    {
                        person: { id: 'person-b' },
                        role: { code: 'role-b' },
                        state: 'waiting_approval',
                        fromDepartment: { name: 'value' },
                        serviceMemberDepartmentId: 1
                    }
                ]
            }
        ];
        var actual = createScopes([
            {
                person: { id: 'person-a' },
                role: { code: 'role-a' },
                state: 'state-a',
                fromDepartment: { name: 'value' },
                serviceMemberDepartmentId: 1
            },
            {
                person: { id: 'person-b' },
                role: { code: 'product_head' },
                state: 'state-a'
            },
            {
                person: { id: 'person-b' },
                role: { code: 'role-b' },
                state: 'waiting_approval',
                fromDepartment: { name: 'value' },
                serviceMemberDepartmentId: 1
            }
        ], 'dept');

        expect(actual).toEqual(expected);
    });

    it('countSize', function() {
        var expected = 4;
        var actual = createScopes.countSize([
            { person: { id: 123 } },
            { person: { id: 111 } },
            { person: { id: 777 } },
            { person: { id: 123 } },
            { person: { id: 696 } }
        ]);

        expect(actual).toEqual(expected);
    });
});
