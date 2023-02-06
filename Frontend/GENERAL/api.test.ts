import { promisedRequest } from '../lib/request';
import { StVersionClient } from './api';

jest.mock('../lib/request');

let client;

const mockPromisedRequest = promisedRequest as jest.MockedFunction<typeof promisedRequest>;
const mockImplementation = params => {
    switch (params.url) {
        case '//api/queues/queue/versions':
            return Promise.resolve([{ name: 'name', description: 'description' }]);
        case '//api/versions':
            return Promise.resolve({ name: 'package@version' });
        case '//api/issues/queue-123':
            return Promise.resolve({ key: 'queue-123', fixVersions: [{ display: 'package@version' }] });
        case '//api/issues/queue-321':
            return Promise.resolve({ key: 'queue-321', fixVersions: [{ display: 'package@version@0.0.1' }] });
        default:
            throw Error(`SET MOCK IMPLEMENTATION FOR ${params.url}`);
    }
};

describe('st-api', () => {
    beforeEach(() => {
        client = new StVersionClient('token', 'queue');
        mockPromisedRequest.mockClear();
    });

    it('constuctor', () => {
        expect(client.queue).toEqual('queue');
        expect(client.token).toEqual('token');
        expect(client.authHeader).toMatchObject({
            Authorization: 'OAuth token',
            'Content-Type': 'application/json',
        });
    });

    it('default params', () => {
        client = new StVersionClient();
        expect(client.queue).toEqual('DEFAULTQUEUE');
        expect(client.token).toEqual('ST_TKN');
    });

    describe('versionExist', () => {
        it('return false, if not exist', async() => {
            expect(client.versionExist([], 'lib', 'version')).toBe(false);
        });
        it('return true, if exist by version', async() => {
            const versions = [{ name: 'version' }];
            expect(client.versionExist(versions, 'lib', 'version')).toBe(true);
        });
        it('return true, if exist by libname@version', async() => {
            const versions = [{ name: 'lib@version' }];
            expect(client.versionExist(versions, 'lib', 'version')).toBe(true);
        });
    });

    describe('getVersionFromArray', () => {
        it('return undefined, if not exist', async() => {
            expect(client.getVersionFromArray([], 'lib', 'version')).toBe(undefined);
        });
        it('return version, if exist by version', async() => {
            const versions = [{ name: 'version' }, { name: 'ver2' }];
            expect(client.getVersionFromArray(versions, 'lib', 'version')).toBe(versions[0]);
        });
        it('return version, if exist by libname@version', async() => {
            const versions = [{ name: 'lib@version' }, { name: 'ver2' }];
            expect(client.getVersionFromArray(versions, 'lib', 'version')).toBe(versions[0]);
        });
    });

    it('getIssueUrl()', async() => {
        expect(client.getIssueUrl()).toEqual('//api/issues/');
        expect(client.getIssueUrl('issue-1')).toEqual('//api/issues/issue-1');
    });

    it('filterQuequeIssues()', async() => {
        const emptyIssues = ['DFLTMDSQ-1', 'ISL-123', 'SERP-456'];
        expect(client.filterQuequeIssues(emptyIssues).length).toEqual(0);

        const issues = ['queue-123', 'ISL-123', 'SERP-456'];
        expect(client.filterQuequeIssues(issues)).toContain('queue-123');
        expect(client.filterQuequeIssues(issues).length).toEqual(1);
    });

    describe('getVersions()', () => {
        it('throw error, if result incorrect', async() => {
            mockPromisedRequest.mockReturnValue(Promise.resolve('nothing'));

            try {
                await client.getVersions();
            } catch (e) {
                expect(e.message).toEqual('\"nothing\" incorrect.');
            }

            expect(mockPromisedRequest).toHaveBeenCalledWith({
                options:
                {
                    headers: {
                        Authorization: 'OAuth token',
                        'Content-Type': 'application/json',
                    },
                    method: 'GET',
                },
                url: '//api/queues/queue/versions',
            });
        });
        it('return empty array', async() => {
            mockPromisedRequest.mockReturnValue(Promise.resolve([]));
            const client = new StVersionClient('token', 'queue');
            const versions = await client.getVersions();
            expect(Array.isArray(versions)).toBe(true);
        });
    });

    it('createNewVersion', async() => {
        mockPromisedRequest.mockReturnValue(Promise.resolve({ name: '123', id: '123' }));
        await client.createNewVersion();

        expect(mockPromisedRequest).toHaveBeenCalledWith({
            options:
            {
                headers: {
                    Authorization: 'OAuth token',
                    'Content-Type': 'application/json',
                },
                method: 'POST',
                json: {
                    queue: 'queue',
                },
            },
            url: '//api/versions',
        });
    });

    it('setFixVersion()', async() => {
        mockPromisedRequest.mockReturnValue(Promise.resolve(''));
        await client.setFixVersion('ISL-123', '@yandex/ui@1.0.0');

        expect(mockPromisedRequest).toHaveBeenCalledWith({
            options:
            {
                headers: {
                    Authorization: 'OAuth token',
                    'Content-Type': 'application/json',
                },
                method: 'PATCH',
                json: {
                    fixVersions: {
                        add: ['@yandex/ui@1.0.0'],
                    },
                },
            },
            url: '//api/issues/ISL-123',
        });
    });

    it('removeFixVersion()', async() => {
        mockPromisedRequest.mockReturnValue(Promise.resolve(''));
        await client.removeFixVersion('ISL-123', [123]);

        expect(mockPromisedRequest).toHaveBeenCalledWith({
            options:
            {
                headers: {
                    Authorization: 'OAuth token',
                    'Content-Type': 'application/json',
                },
                method: 'PATCH',
                json: {
                    fixVersions: {
                        remove: [123],
                    },
                },
            },
            url: '//api/issues/ISL-123',
        });
    });

    it('getIssueInfo()', async() => {
        mockPromisedRequest.mockReturnValue(Promise.resolve({ key: 'version' }));
        await client.getIssueInfo('ISL-123');

        expect(mockPromisedRequest).toHaveBeenCalledWith({
            options:
            {
                headers: {
                    Authorization: 'OAuth token',
                    'Content-Type': 'application/json',
                },
                method: 'GET',
            },
            url: '//api/issues/ISL-123',
        });
    });

    describe('setSTVersion()', () => {
        it('throw error, if version not exist and createIfNoExist=false', async() => {
            mockPromisedRequest.mockImplementation(mockImplementation);

            const params = {
                issues: ['ISL-123'],
                npmName: 'package',
                npmVersion: 'version',
                createIfNoExist: false,
            };
            try {
                await client.setSTVersion(params);
            } catch (e) {
                expect(e.message).toContain('not exist. Set option createIfNoExist = true, or create version manually.');
            }
        });
        it('should set version, if version exist', async() => {
            mockPromisedRequest.mockImplementation(mockImplementation);

            const params = {
                issues: ['queue-123'],
                npmName: 'package',
                npmVersion: 'name',
            };

            await client.setSTVersion(params);
        });
        it('should set versions', async() => {
            mockPromisedRequest.mockImplementation(mockImplementation);

            const params = {
                issues: ['queue-123', 'queue-321'],
                npmName: 'package',
                npmVersion: 'version',
            };

            await client.setSTVersion(params);
        });
    });
});
