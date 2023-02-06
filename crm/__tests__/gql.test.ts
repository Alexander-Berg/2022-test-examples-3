import { createGrpc } from '@crm/apphost-test';
import {
    departmentmanager,
    NAppHostHttp,
    organizationmanager,
    usermanager,
} from '@crm/protos';
import { Config } from 'services/Config';
import { setupServer } from 'msw/node';
import { rest } from 'msw';
import { createApp } from '../createApp';

const { THttpRequest } = NAppHostHttp;

const server = setupServer(
    rest.get(`${Config.defaultGallifreyUrl}/users/list`, (req, res, ctx) => {
        return res(
            ctx.status(200),
            ctx.body(
                usermanager.UsersResponse.encode({
                    users: [
                        {
                            userId: 1,
                            data: {
                                firstName: 'Test',
                                secondName: 'Test',
                            },
                        },
                        {
                            userId: 2,
                            data: {
                                firstName: 'Test',
                                secondName: 'Test',
                            },
                        },
                    ],
                }).finish(),
            ),
        );
    }),

    rest.get(`${Config.defaultGallifreyUrl}/user/:id`, (req, res, ctx) => {
        const userId = parseInt(req.params.id as string, 10);

        if (userId === -1) {
            return res(
                ctx.status(200),
                ctx.body(usermanager.UserResponse.encode({}).finish()),
            );
        }

        return res(
            ctx.status(200),
            ctx.body(
                usermanager.UserResponse.encode({
                    user: {
                        userId,
                        data: {
                            firstName: 'Test',
                            secondName: 'Test',
                        },
                    },
                }).finish(),
            ),
        );
    }),

    rest.put(`${Config.defaultGallifreyUrl}/user/create`, (req, res, ctx) => {
        const body = Buffer.from(req.body as string, 'utf-8');
        const data = usermanager.UserData.decode(body);

        return res(
            ctx.status(200),
            ctx.body(
                usermanager.UserResponse.encode({
                    user: {
                        userId: 1,
                        data: {
                            firstName: data.firstName,
                            secondName: data.secondName,
                        },
                    },
                }).finish(),
            ),
        );
    }),

    rest.patch(
        `${Config.defaultGallifreyUrl}/user/:id/update`,
        (req, res, ctx) => {
            const userId = parseInt(req.params.id as string, 10);
            const body = Buffer.from(req.body as string, 'utf-8');
            const data = usermanager.UserData.decode(body);

            return res(
                ctx.status(200),
                ctx.body(
                    usermanager.UserResponse.encode({
                        user: {
                            userId,
                            data: {
                                firstName: data.firstName,
                                secondName: data.secondName,
                            },
                        },
                    }).finish(),
                ),
            );
        },
    ),

    rest.post(
        `${Config.defaultGallifreyUrl}/user/:id/archive`,
        (req, res, ctx) => {
            const userId = parseInt(req.params.id as string, 10);

            return res(
                ctx.status(200),
                ctx.body(
                    usermanager.UserResponse.encode({
                        user: {
                            userId,
                            data: {
                                firstName: 'Archived name',
                                secondName: 'Archived last name',
                            },
                        },
                    }).finish(),
                ),
            );
        },
    ),

    rest.get(
        `${Config.defaultGallifreyUrl}/organizations/list`,
        (req, res, ctx) => {
            return res(
                ctx.status(200),
                ctx.body(
                    organizationmanager.OrganizationsResponse.encode({
                        organizations: [
                            {
                                id: {
                                    value: 1,
                                },
                                data: {
                                    pool: 1,
                                    slug: 'Test',
                                    name: 'Test',
                                },
                            },
                            {
                                id: {
                                    value: 2,
                                },
                                data: {
                                    pool: 1,
                                    slug: 'Test',
                                    name: 'Test',
                                },
                            },
                        ],
                    }).finish(),
                ),
            );
        },
    ),

    rest.get(
        `${Config.defaultGallifreyUrl}/organization/:id`,
        (req, res, ctx) => {
            const id = parseInt(req.params.id as string, 10);

            if (id === -1) {
                return res(
                    ctx.status(200),
                    ctx.body(
                        organizationmanager.OrganizationResponse.encode(
                            {},
                        ).finish(),
                    ),
                );
            }

            return res(
                ctx.status(200),
                ctx.body(
                    organizationmanager.OrganizationResponse.encode({
                        organization: {
                            id: {
                                value: id,
                            },
                            data: {
                                pool: 1,
                                slug: 'Test',
                                name: 'Test',
                            },
                        },
                    }).finish(),
                ),
            );
        },
    ),

    rest.get(
        `${Config.defaultGallifreyUrl}/user/organization/list/:id`,
        (req, res, ctx) => {
            const id = parseInt(req.params.id as string, 10);
            const pattern = req.url.searchParams.get('pattern');

            if (id === -1) {
                return res(
                    ctx.status(200),
                    ctx.body(
                        usermanager.OrganizationUsersResponse.encode(
                            {},
                        ).finish(),
                    ),
                );
            }

            const usersId = [1, 2, 3];
            if (pattern != null) {
                usersId.splice(1, 2);
            }

            return res(
                ctx.status(200),
                ctx.body(
                    usermanager.OrganizationUsersResponse.encode({
                        usersId,
                    }).finish(),
                ),
            );
        },
    ),

    rest.put(
        `${Config.defaultGallifreyUrl}/organization/create`,
        (req, res, ctx) => {
            const body = Buffer.from(req.body as string, 'utf-8');
            const data = organizationmanager.OrganizationData.decode(body);

            return res(
                ctx.status(200),
                ctx.body(
                    organizationmanager.OrganizationResponse.encode({
                        organization: {
                            id: {
                                value: 1,
                            },
                            data: {
                                slug: data.slug,
                                pool: data.pool,
                                name: data.name,
                            },
                        },
                    }).finish(),
                ),
            );
        },
    ),

    rest.patch(
        `${Config.defaultGallifreyUrl}/organization/update/:id`,
        (req, res, ctx) => {
            const id = parseInt(req.params.id as string, 10);
            const body = Buffer.from(req.body as string, 'utf-8');
            const data =
                organizationmanager.UpdateOrganizationData.decode(body);

            return res(
                ctx.status(200),
                ctx.body(
                    organizationmanager.OrganizationResponse.encode({
                        organization: {
                            id: {
                                value: id,
                            },
                            data: {
                                pool: data.newPool,
                                slug: data.newSlug,
                                name: data.newName,
                            },
                        },
                    }).finish(),
                ),
            );
        },
    ),

    rest.post(
        `${Config.defaultGallifreyUrl}/organization/archive/:id`,
        (req, res, ctx) => {
            const id = parseInt(req.params.id as string, 10);

            return res(
                ctx.status(200),
                ctx.body(
                    organizationmanager.OrganizationResponse.encode({
                        organization: {
                            id: {
                                value: id,
                            },
                            data: {
                                pool: 1,
                                slug: 'Archived slug',
                                name: 'Archived name',
                            },
                        },
                    }).finish(),
                ),
            );
        },
    ),

    rest.put(
        `${Config.defaultGallifreyUrl}/user/organization/add/:orgId/:userId`,
        (req, res, ctx) => {
            return res(ctx.status(200), ctx.body(''));
        },
    ),

    rest.delete(
        `${Config.defaultGallifreyUrl}/user/organization/remove/:orgId/:userId`,
        (req, res, ctx) => {
            return res(ctx.status(200), ctx.body(''));
        },
    ),

    rest.get(
        `${Config.defaultGallifreyUrl}/department/list`,
        (req, res, ctx) => {
            return res(
                ctx.status(200),
                ctx.body(
                    departmentmanager.DepartmentsResponse.encode({
                        departments: [
                            {
                                id: '2',
                                data: {
                                    parentDepartmentId: '1',
                                    organizationId: 1,
                                    names: [
                                        {
                                            languageCode: '1',
                                            name: '1',
                                        },
                                    ],
                                },
                            },
                            {
                                id: '3',
                                data: {
                                    parentDepartmentId: '1',
                                    organizationId: 2,
                                    names: [
                                        {
                                            languageCode: '1',
                                            name: '1',
                                        },
                                    ],
                                },
                            },
                        ],
                    }).finish(),
                ),
            );
        },
    ),

    rest.get(
        `${Config.defaultGallifreyUrl}/department/:orgId/list`,
        (req, res, ctx) => {
            const orgId = parseInt(req.params.orgId as string, 10);
            const pattern = req.url.searchParams.get('pattern');
            const includeUsersCount =
                req.url.searchParams.get('includeUsersCount');

            const usersCount: departmentmanager.IDepartmentUsersCount[] =
                includeUsersCount === 'true'
                    ? [
                          {
                              organizationId: orgId,
                              departmentId: '1',
                              usersCount: 10,
                          },
                          {
                              organizationId: orgId,
                              departmentId: '2',
                              usersCount: 20,
                          },
                      ]
                    : [];

            const departments = [
                {
                    id: '1',
                    data: {
                        parentDepartmentId: '1',
                        organizationId: orgId,
                        names: [
                            {
                                languageCode: '1',
                                name: '1',
                            },
                        ],
                    },
                },
                {
                    id: '2',
                    data: {
                        parentDepartmentId: '2',
                        organizationId: orgId,
                        names: [
                            {
                                languageCode: '2',
                                name: '2',
                            },
                        ],
                    },
                },
            ];

            if (pattern != null) {
                departments.splice(1, 1);
            }

            return res(
                ctx.status(200),
                ctx.body(
                    departmentmanager.DepartmentsResponse.encode({
                        departments,
                        usersCount,
                    }).finish(),
                ),
            );
        },
    ),

    rest.get(
        `${Config.defaultGallifreyUrl}/department/:orgId/:departmentId`,
        (req, res, ctx) => {
            const orgId = parseInt(req.params.orgId as string, 10);
            const includeUsersCount =
                req.url.searchParams.get('includeUsersCount');
            const departmentId = req.params.departmentId as string;

            const usersCount: departmentmanager.IDepartmentUsersCount[] =
                includeUsersCount === 'true'
                    ? [
                          {
                              organizationId: orgId,
                              departmentId,
                              usersCount: 10,
                          },
                      ]
                    : [];

            return res(
                ctx.status(200),
                ctx.body(
                    departmentmanager.DepartmentsResponse.encode({
                        departments: [
                            {
                                id: departmentId,
                                data: {
                                    parentDepartmentId: '1',
                                    organizationId: orgId,
                                    names: [
                                        {
                                            languageCode: '1',
                                            name: '1',
                                        },
                                    ],
                                },
                            },
                        ],
                        usersCount,
                    }).finish(),
                ),
            );
        },
    ),

    rest.get(
        `${Config.defaultGallifreyUrl}/department/:orgId/:departmentId/users`,
        (req, res, ctx) => {
            return res(
                ctx.status(200),
                ctx.body(
                    departmentmanager.DepartmentUsersResponse.encode({
                        usersId: [1, 2, 3],
                    }).finish(),
                ),
            );
        },
    ),

    rest.get(
        `${Config.defaultGallifreyUrl}/department/:orgId/:departmentId/responsible`,
        (req, res, ctx) => {
            return res(
                ctx.status(200),
                ctx.body(
                    departmentmanager.DepartmentUsersResponse.encode({
                        usersId: [1, 2],
                    }).finish(),
                ),
            );
        },
    ),

    rest.patch(
        `${Config.defaultGallifreyUrl}/department/create`,
        (req, res, ctx) => {
            const body = Buffer.from(req.body as string, 'utf-8');
            const data = departmentmanager.CreateDepartmentRequest.decode(body);

            return res(
                ctx.status(200),
                ctx.body(
                    departmentmanager.DepartmentsResponse.encode({
                        departments: [
                            {
                                id: '1',
                                data: data.departmentData,
                            },
                        ],
                    }).finish(),
                ),
            );
        },
    ),

    rest.put(
        `${Config.defaultGallifreyUrl}/department/:orgId/:departmentId/update`,
        (req, res, ctx) => {
            const departmentId = req.params.departmentId as string;
            const body = Buffer.from(req.body as string, 'utf-8');
            const data = departmentmanager.UpdateDepartmentData.decode(body);

            return res(
                ctx.status(200),
                ctx.body(
                    departmentmanager.DepartmentsResponse.encode({
                        departments: [
                            {
                                id: departmentId,
                                data: {
                                    parentDepartmentId:
                                        data.newParentDepartmentId,
                                    organizationId: Number(
                                        data.newOrganizationId,
                                    ),
                                    names: data.newNames,
                                },
                            },
                        ],
                    }).finish(),
                ),
            );
        },
    ),

    rest.post(
        `${Config.defaultGallifreyUrl}/department/:orgId/:departmentId/archive`,
        (req, res, ctx) => {
            return res(ctx.status(200));
        },
    ),

    rest.put(
        `${Config.defaultGallifreyUrl}/department/:orgId/:departmentId/add/user/:userId`,
        (req, res, ctx) => {
            return res(ctx.status(200));
        },
    ),

    rest.delete(
        `${Config.defaultGallifreyUrl}/department/:orgId/:departmentId/remove/user/:userId`,
        (req, res, ctx) => {
            return res(ctx.status(200));
        },
    ),

    rest.put(
        `${Config.defaultGallifreyUrl}/department/:orgId/:departmentId/add/responsible/:userId`,
        (req, res, ctx) => {
            return res(ctx.status(200));
        },
    ),

    rest.delete(
        `${Config.defaultGallifreyUrl}/department/:orgId/:departmentId/remove/responsible/:userId`,
        (req, res, ctx) => {
            return res(ctx.status(200));
        },
    ),
);

beforeAll(() => server.listen({ onUnhandledRequest: 'bypass' }));
afterAll(() => server.close());

describe('/graphql', () => {
    const grpc = createGrpc(createApp);
    const gqlRequest = (query: string, results: object[] = []) => {
        return grpc('/graphql', {
            context: [
                {
                    name: 'REQUEST',
                    results: [
                        {
                            type: 'proto_http_request',
                            binary: {
                                Method: THttpRequest.EMethod.Post,
                                Content: Buffer.from(
                                    JSON.stringify({
                                        query,
                                    }),
                                ).toString('base64'),
                            },
                            __content_type: 'json',
                        },
                        {
                            type: 'tvm_user_ticket',
                            binary: {
                                UserTicket: '123',
                            },
                            __content_type: 'json',
                        },
                        ...results,
                    ],
                },
            ],
        });
    };

    describe('simple', () => {
        it('returns me', async () => {
            const response = await gqlRequest('{ me }', [
                {
                    type: 'blackbox_user',
                    binary: {
                        Login: 'login',
                    },
                    __content_type: 'json',
                },
            ]);

            const content = Buffer.from(
                response.answers[0].Content,
                'base64',
            ).toString('ascii');
            expect(JSON.parse(content)).toStrictEqual({
                data: { me: 'login' },
            });
        });

        it('returns hello', async () => {
            const response = await gqlRequest('{ hello }');

            const content = Buffer.from(
                response.answers[0].Content,
                'base64',
            ).toString('ascii');
            expect(JSON.parse(content)).toStrictEqual({
                data: { hello: 'Hello world!' },
            });
        });
    });

    describe('users', () => {
        it('returns users', async () => {
            const response = await gqlRequest(`
                {
                    users {
                        id
                        firstName
                        lastName
                    }
                }
            `);

            const content = Buffer.from(
                response.answers[0].Content,
                'base64',
            ).toString('ascii');
            expect(JSON.parse(content)).toStrictEqual({
                data: {
                    users: [
                        {
                            id: 1,
                            firstName: 'Test',
                            lastName: 'Test',
                        },
                        {
                            id: 2,
                            firstName: 'Test',
                            lastName: 'Test',
                        },
                    ],
                },
            });
        });

        it('returns user', async () => {
            const response = await gqlRequest(`
                {
                    user(id: 10) {
                        id
                        firstName
                        lastName
                    }
                }
            `);

            const content = Buffer.from(
                response.answers[0].Content,
                'base64',
            ).toString('ascii');
            expect(JSON.parse(content)).toStrictEqual({
                data: {
                    user: {
                        id: 10,
                        firstName: 'Test',
                        lastName: 'Test',
                    },
                },
            });
        });

        it('returns null user when user is not found', async () => {
            const response = await gqlRequest(`
                {
                    user(id: -1) {
                        id
                        firstName
                        lastName
                    }
                }
            `);

            const content = JSON.parse(
                Buffer.from(response.answers[0].Content, 'base64').toString(
                    'ascii',
                ),
            );

            expect(content.data).toBeNull();
        });

        it('creates user', async () => {
            const response = await gqlRequest(`
                mutation {
                    createUser(input: {
                        firstName: "Name",
                        lastName: "LastName"
                    }) {
                        id
                        firstName
                        lastName
                    }
                }
            `);

            const content = Buffer.from(
                response.answers[0].Content,
                'base64',
            ).toString('ascii');
            expect(JSON.parse(content)).toStrictEqual({
                data: {
                    createUser: {
                        id: 1,
                        firstName: 'Name',
                        lastName: 'LastName',
                    },
                },
            });
        });

        it('updates user', async () => {
            const response = await gqlRequest(`
                mutation {
                    updateUser(id: 1, input: {
                        firstName: "Changed name",
                        lastName: "Changed last name"
                    }) {
                        id
                        firstName
                        lastName
                    }
                }
            `);

            const content = Buffer.from(
                response.answers[0].Content,
                'base64',
            ).toString('ascii');
            expect(JSON.parse(content)).toStrictEqual({
                data: {
                    updateUser: {
                        id: 1,
                        firstName: 'Changed name',
                        lastName: 'Changed last name',
                    },
                },
            });
        });

        it('archives user', async () => {
            const response = await gqlRequest(`
                mutation {
                    archiveUser(id: 1) {
                        id
                        firstName
                        lastName
                    }
                }
            `);

            const content = Buffer.from(
                response.answers[0].Content,
                'base64',
            ).toString('ascii');
            expect(JSON.parse(content)).toStrictEqual({
                data: {
                    archiveUser: {
                        id: 1,
                        firstName: 'Archived name',
                        lastName: 'Archived last name',
                    },
                },
            });
        });
    });

    describe('organizations', () => {
        it('returns organizations', async () => {
            const response = await gqlRequest(`
                {
                    organizations {
                        id
                        pool
                        slug
                        name
                    }
                }
            `);

            const content = Buffer.from(
                response.answers[0].Content,
                'base64',
            ).toString('ascii');
            expect(JSON.parse(content)).toStrictEqual({
                data: {
                    organizations: [
                        {
                            id: 1,
                            pool: 1,
                            slug: 'Test',
                            name: 'Test',
                        },
                        {
                            id: 2,
                            pool: 1,
                            slug: 'Test',
                            name: 'Test',
                        },
                    ],
                },
            });
        });

        it('returns organization', async () => {
            const response = await gqlRequest(`
                {
                    organization(id: 10) {
                        id
                        pool
                        slug
                        name
                    }
                }
            `);

            const content = Buffer.from(
                response.answers[0].Content,
                'base64',
            ).toString('ascii');
            expect(JSON.parse(content)).toStrictEqual({
                data: {
                    organization: {
                        id: 10,
                        pool: 1,
                        slug: 'Test',
                        name: 'Test',
                    },
                },
            });
        });

        it('returns organization users', async () => {
            const response = await gqlRequest(`
                {
                    organization(id: 10) {
                        users {
                            id
                            firstName
                        }
                    }
                }
            `);

            const content = Buffer.from(
                response.answers[0].Content,
                'base64',
            ).toString('ascii');
            expect(JSON.parse(content)).toStrictEqual({
                data: {
                    organization: {
                        users: [
                            {
                                id: 1,
                                firstName: 'Test',
                            },
                            {
                                id: 2,
                                firstName: 'Test',
                            },
                            {
                                id: 3,
                                firstName: 'Test',
                            },
                        ],
                    },
                },
            });
        });

        it('returns organization users with filter', async () => {
            const response = await gqlRequest(`
                {
                    organization(id: 10) {
                        users(filter: {
                            text: "Filter test"
                        }) {
                            id
                            firstName
                        }
                    }
                }
            `);

            const content = Buffer.from(
                response.answers[0].Content,
                'base64',
            ).toString('ascii');
            expect(JSON.parse(content)).toStrictEqual({
                data: {
                    organization: {
                        users: [
                            {
                                id: 1,
                                firstName: 'Test',
                            },
                        ],
                    },
                },
            });
        });

        it('creates organization', async () => {
            const response = await gqlRequest(`
                mutation {
                    createOrganization(input: {
                        pool: 2,
                        slug: "Test",
                        name: "Test"
                    }) {
                        id
                        pool
                        slug
                        name
                    }
                }
            `);

            const content = Buffer.from(
                response.answers[0].Content,
                'base64',
            ).toString('ascii');
            expect(JSON.parse(content)).toStrictEqual({
                data: {
                    createOrganization: {
                        id: 1,
                        pool: 2,
                        slug: 'Test',
                        name: 'Test',
                    },
                },
            });
        });

        it('updates organization', async () => {
            const response = await gqlRequest(`
                mutation {
                    updateOrganization(id: 1, input: {
                        pool: 2,
                        slug: "Changed slug",
                        name: "Changed name"
                    }) {
                        id
                        pool
                        slug
                        name
                    }
                }
            `);

            const content = Buffer.from(
                response.answers[0].Content,
                'base64',
            ).toString('ascii');
            expect(JSON.parse(content)).toStrictEqual({
                data: {
                    updateOrganization: {
                        id: 1,
                        pool: 2,
                        slug: 'Changed slug',
                        name: 'Changed name',
                    },
                },
            });
        });

        it('archives organization', async () => {
            const response = await gqlRequest(`
                mutation {
                    archiveOrganization(id: 1) {
                        id
                        pool
                        slug
                        name
                    }
                }
            `);

            const content = Buffer.from(
                response.answers[0].Content,
                'base64',
            ).toString('ascii');
            expect(JSON.parse(content)).toStrictEqual({
                data: {
                    archiveOrganization: {
                        id: 1,
                        pool: 1,
                        slug: 'Archived slug',
                        name: 'Archived name',
                    },
                },
            });
        });

        it('adds user to organization', async () => {
            const response = await gqlRequest(`
                mutation {
                    addUserToOrganization(userId: 1, organizationId: 1)
                }
            `);

            const content = Buffer.from(
                response.answers[0].Content,
                'base64',
            ).toString('ascii');
            expect(JSON.parse(content)).toStrictEqual({
                data: {
                    addUserToOrganization: true,
                },
            });
        });

        it('removes user from organization', async () => {
            const response = await gqlRequest(`
                mutation {
                    removeUserFromOrganization(userId: 1, organizationId: 1)
                }
            `);

            const content = Buffer.from(
                response.answers[0].Content,
                'base64',
            ).toString('ascii');
            expect(JSON.parse(content)).toStrictEqual({
                data: {
                    removeUserFromOrganization: true,
                },
            });
        });
    });

    describe('departments', () => {
        it('returns departments', async () => {
            const response = await gqlRequest(`
                {
                    departments {
                        id
                        parentDepartmentId
                        organizationId
                        names {
                            languageCode
                            name
                        }
                    }
                }
            `);

            const content = Buffer.from(
                response.answers[0].Content,
                'base64',
            ).toString('ascii');
            expect(JSON.parse(content)).toStrictEqual({
                data: {
                    departments: [
                        {
                            id: '2',
                            parentDepartmentId: '1',
                            organizationId: 1,
                            names: [
                                {
                                    languageCode: '1',
                                    name: '1',
                                },
                            ],
                        },
                        {
                            id: '3',
                            parentDepartmentId: '1',
                            organizationId: 2,
                            names: [
                                {
                                    languageCode: '1',
                                    name: '1',
                                },
                            ],
                        },
                    ],
                },
            });
        });

        it('returns all organization departments', async () => {
            const response = await gqlRequest(`
                {
                    organization(id: 10) {
                        departments {
                            id
                            parentDepartmentId
                            organizationId
                            names {
                                languageCode
                                name
                            }
                            usersCount
                        }
                    }
                }
            `);

            const content = Buffer.from(
                response.answers[0].Content,
                'base64',
            ).toString('ascii');
            expect(JSON.parse(content)).toStrictEqual({
                data: {
                    organization: {
                        departments: [
                            {
                                id: '1',
                                parentDepartmentId: '1',
                                organizationId: 10,
                                names: [
                                    {
                                        languageCode: '1',
                                        name: '1',
                                    },
                                ],
                                usersCount: 10,
                            },
                            {
                                id: '2',
                                parentDepartmentId: '2',
                                organizationId: 10,
                                names: [
                                    {
                                        languageCode: '2',
                                        name: '2',
                                    },
                                ],
                                usersCount: 20,
                            },
                        ],
                    },
                },
            });
        });

        it('returns all organization departments with text filter', async () => {
            const response = await gqlRequest(`
                {
                    organization(id: 10) {
                        departments(filter: {
                            text: "1"
                        }) {
                            id
                            parentDepartmentId
                            organizationId
                            names {
                                languageCode
                                name
                            }
                            usersCount
                        }
                    }
                }
            `);

            const content = Buffer.from(
                response.answers[0].Content,
                'base64',
            ).toString('ascii');
            expect(JSON.parse(content)).toStrictEqual({
                data: {
                    organization: {
                        departments: [
                            {
                                id: '1',
                                parentDepartmentId: '1',
                                organizationId: 10,
                                names: [
                                    {
                                        languageCode: '1',
                                        name: '1',
                                    },
                                ],
                                usersCount: 10,
                            },
                        ],
                    },
                },
            });
        });

        it('returns one organization department', async () => {
            const response = await gqlRequest(`
                {
                    organization(id: 10) {
                        department(id: "20") {
                            id
                            parentDepartmentId
                            organizationId
                            names {
                                languageCode
                                name
                            }
                            usersCount
                        }
                    }
                }
            `);

            const content = Buffer.from(
                response.answers[0].Content,
                'base64',
            ).toString('ascii');
            expect(JSON.parse(content)).toStrictEqual({
                data: {
                    organization: {
                        department: {
                            id: '20',
                            parentDepartmentId: '1',
                            organizationId: 10,
                            names: [
                                {
                                    languageCode: '1',
                                    name: '1',
                                },
                            ],
                            usersCount: 10,
                        },
                    },
                },
            });
        });

        it('returns all department users', async () => {
            const response = await gqlRequest(`
                {
                    organization(id: 10) {
                        department(id: "20") {
                            users {
                                id
                                firstName
                            }
                        }
                    }
                }
            `);

            const content = Buffer.from(
                response.answers[0].Content,
                'base64',
            ).toString('ascii');
            expect(JSON.parse(content)).toStrictEqual({
                data: {
                    organization: {
                        department: {
                            users: [
                                {
                                    id: 1,
                                    firstName: 'Test',
                                },
                                {
                                    id: 2,
                                    firstName: 'Test',
                                },
                                {
                                    id: 3,
                                    firstName: 'Test',
                                },
                            ],
                        },
                    },
                },
            });
        });

        it('returns all department responsibles', async () => {
            const response = await gqlRequest(`
                {
                    organization(id: 10) {
                        department(id: "20") {
                            responsibles {
                                id
                                firstName
                            }
                        }
                    }
                }
            `);

            const content = Buffer.from(
                response.answers[0].Content,
                'base64',
            ).toString('ascii');
            expect(JSON.parse(content)).toStrictEqual({
                data: {
                    organization: {
                        department: {
                            responsibles: [
                                {
                                    id: 1,
                                    firstName: 'Test',
                                },
                                {
                                    id: 2,
                                    firstName: 'Test',
                                },
                            ],
                        },
                    },
                },
            });
        });

        it('creates department', async () => {
            const response = await gqlRequest(`
                mutation {
                    createDepartment(input: {
                        organizationId: 1,
                        parentDepartmentId: "2",
                        names: [],
                        responsibleIds: [1]
                    }) {
                        id
                        organizationId
                        parentDepartmentId
                        names {
                            name
                        }
                    }
                }
            `);

            const content = Buffer.from(
                response.answers[0].Content,
                'base64',
            ).toString('ascii');
            expect(JSON.parse(content)).toStrictEqual({
                data: {
                    createDepartment: {
                        id: '1',
                        organizationId: 1,
                        parentDepartmentId: '2',
                        names: [],
                    },
                },
            });
        });

        it('updates department', async () => {
            const response = await gqlRequest(`
                mutation {
                    updateDepartment(organizationId: 1, departmentId: "1", input: {
                        parentDepartmentId: "10",
                        organizationId: 10,
                        names: [
                            {
                                languageCode: "10",
                                name: "10",
                            }
                        ]
                    }) {
                        id
                        parentDepartmentId
                        organizationId
                        names {
                            languageCode
                            name
                        }
                    }
                }
            `);

            const content = Buffer.from(
                response.answers[0].Content,
                'base64',
            ).toString('ascii');
            expect(JSON.parse(content)).toStrictEqual({
                data: {
                    updateDepartment: {
                        id: '1',
                        parentDepartmentId: '10',
                        organizationId: 10,
                        names: [
                            {
                                languageCode: '10',
                                name: '10',
                            },
                        ],
                    },
                },
            });
        });

        it('archives department', async () => {
            const response = await gqlRequest(`
                mutation {
                    archiveDepartment(organizationId: 1, departmentId: "1")
                }
            `);

            const content = Buffer.from(
                response.answers[0].Content,
                'base64',
            ).toString('ascii');
            expect(JSON.parse(content)).toStrictEqual({
                data: {
                    archiveDepartment: true,
                },
            });
        });

        it('adds user to department', async () => {
            const response = await gqlRequest(`
                mutation {
                    addUserToDepartment(organizationId: 1, departmentId: "1", userId: 1)
                }
            `);

            const content = Buffer.from(
                response.answers[0].Content,
                'base64',
            ).toString('ascii');
            expect(JSON.parse(content)).toStrictEqual({
                data: {
                    addUserToDepartment: true,
                },
            });
        });

        it('removes user from department', async () => {
            const response = await gqlRequest(`
                mutation {
                    removeUserFromDepartment(organizationId: 1, departmentId: "1", userId: 1)
                }
            `);

            const content = Buffer.from(
                response.answers[0].Content,
                'base64',
            ).toString('ascii');
            expect(JSON.parse(content)).toStrictEqual({
                data: {
                    removeUserFromDepartment: true,
                },
            });
        });

        it('adds responsible to department', async () => {
            const response = await gqlRequest(`
                mutation {
                    addResponsibleToDepartment(organizationId: 1, departmentId: "1", userId: 1)
                }
            `);

            const content = Buffer.from(
                response.answers[0].Content,
                'base64',
            ).toString('ascii');
            expect(JSON.parse(content)).toStrictEqual({
                data: {
                    addResponsibleToDepartment: true,
                },
            });
        });

        it('removes responsible from department', async () => {
            const response = await gqlRequest(`
                mutation {
                    removeResponsibleFromDepartment(organizationId: 1, departmentId: "1", userId: 1)
                }
            `);

            const content = Buffer.from(
                response.answers[0].Content,
                'base64',
            ).toString('ascii');
            expect(JSON.parse(content)).toStrictEqual({
                data: {
                    removeResponsibleFromDepartment: true,
                },
            });
        });
    });
});
