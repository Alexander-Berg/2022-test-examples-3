import debugFactory, { IDebugger } from 'debug';
import SandboxerResource from '@yandex-int/sandboxer/build/resource/resource';
import Requests from '@yandex-int/si.ci.requests';

import DevicesDownloader, {
    DevicesResourceSearchingError,
    DevicesResourceExistingError,
    DevicesFileDownloadingError,
    DevicesResourceLinkExtractingError,
} from './devices-downloader';

import MockedSandboxer from '../../../../test/helpers/sandboxer';

type requestsType = Requests.GotInstance<Requests.GotJSONFn>;

describe('DevicesDownloader', () => {
    let debug: IDebugger;
    let sandbox: MockedSandboxer;
    let requests: requestsType;

    let sandboxFindMock: jest.Mock;
    let requestsMock: jest.Mock;

    beforeEach(() => {
        debug = debugFactory('test');

        sandboxFindMock = jest.fn();
        requestsMock = jest.fn();

        sandbox = new MockedSandboxer({ token: 'fake-token' }, sandboxFindMock);
        requests = requestsMock as unknown as requestsType;
    });

    describe('.downloadDevices', () => {
        it('should return content of downloaded resource', async() => {
            sandboxFindMock.mockResolvedValue({ items: [{ id: 1, http: { proxy: 'test-url' } }] });
            requestsMock.mockResolvedValue({ body: [123] });

            const downloader = new DevicesDownloader({ debug, sandbox, requests });

            const expected = [123];

            const actual = await downloader.downloadDevices();

            expect(sandboxFindMock).toHaveBeenCalled();
            expect(requestsMock).toHaveBeenCalledWith('test-url');
            expect(actual).toEqual(expected);
        });

        it('should throw DevicesResourceSearchingError when failed to find resource', () => {
            sandboxFindMock.mockRejectedValue(new Error('network error'));

            const downloader = new DevicesDownloader({ debug, sandbox, requests });

            const expected = new DevicesResourceSearchingError(new Error('network error'));

            const actual = downloader.downloadDevices();

            return expect(actual).rejects.toThrowError(expected);
        });

        it('should throw DevicesResourceExistingError when resources does not found', () => {
            sandboxFindMock.mockResolvedValue({ items: [] });

            const downloader = new DevicesDownloader({ debug, sandbox, requests });

            const expected = new DevicesResourceExistingError();

            const actual = downloader.downloadDevices();

            return expect(actual).rejects.toThrowError(expected);
        });

        it('should throw DevicesFileDownloadingError when error occurred while downloading resource', () => {
            sandboxFindMock.mockResolvedValue({ items: [{ id: 1, http: { proxy: 'test-url' } }] });
            requestsMock.mockRejectedValue(new Error('network error'));

            const downloader = new DevicesDownloader({ debug, sandbox, requests });

            const expected = new DevicesFileDownloadingError(
                { id: 1 } as unknown as SandboxerResource,
                new Error('network error'),
            );

            const actual = downloader.downloadDevices();

            return expect(actual).rejects.toThrowError(expected);
        });

        it('should throw DevicesResourceLinkExtractingError when failed to extract link from resource', () => {
            sandboxFindMock.mockResolvedValue({ items: [{ id: 1, http: {} }] });

            const downloader = new DevicesDownloader({ debug, sandbox, requests });

            const expected = new DevicesResourceLinkExtractingError({ id: 1 } as unknown as SandboxerResource);

            const actual = downloader.downloadDevices();

            return expect(actual).rejects.toThrowError(expected);
        });
    });
});
