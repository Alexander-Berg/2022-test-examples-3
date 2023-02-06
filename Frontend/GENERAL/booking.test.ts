import debugFactory, { IDebugger } from 'debug';
import Requests from '@yandex-int/si.ci.requests';

import { Project } from '@yandex-int/testpalm-api';
import BookingApiService, {
    CalculateLoadByTestcasesInput,
    BookingAssessorsEnvironmentsDownloadingError,
} from './booking';
import { Launch, LaunchTypes, LaunchStatuses, LaunchId } from '../../../models/Launch';
import DevicesDownloader from './devices-downloader';

type requestsType = Requests.GotInstance<Requests.GotJSONFn>;

function createTestCase(estimatedTime: number): CalculateLoadByTestcasesInput {
    return { estimatedTime };
}

function createLaunch(launch: Partial<Launch> = {}): Launch {
    return {
        _id: 'fake-id' as unknown as LaunchId,
        title: 'test-title',
        project: 'test-project',
        author: 'robot',
        type: LaunchTypes.testsuite,
        content: {
            testsuiteId: 'fake-test-suite',
        },
        status: LaunchStatuses.draft,
        tags: [],
        platform: 'desktop',
        properties: [],
        environments: [],
        bookingId: null,
        testRunIds: [],
        createdAt: 1,
        updatedAt: 1,

        ...launch as Launch,
    };
}

describe('BookingApiService', () => {
    let debug: IDebugger;
    let requests: requestsType;
    let devicesDownloader: DevicesDownloader;

    let requestsMock: jest.Mock;
    let devicesDownloaderMock: jest.Mock;

    beforeEach(() => {
        debug = debugFactory('test');

        requestsMock = jest.fn();
        devicesDownloaderMock = jest.fn();

        devicesDownloader = {
            downloadDevices: devicesDownloaderMock,
        } as unknown as DevicesDownloader;

        requests = requestsMock as unknown as requestsType;
    });

    describe('.calculateLoadByTestcases', () => {
        it('should correctly calculate load', () => {
            const bookingApi = new BookingApiService({ debug, requests, devicesDownloader });

            const testcases = Array(5).fill(createTestCase(6596000));

            const expected = 14;

            const actual = bookingApi.calculateLoadByTestcases(testcases, createLaunch({ environments: ['Chrome'] }));

            expect(actual).toStrictEqual(expected);
        });

        it('should count by number of environments', () => {
            const bookingApi = new BookingApiService({ debug, requests, devicesDownloader });

            const testcases = Array(5).fill(createTestCase(6596000));

            const expected = 28;

            const actual = bookingApi.calculateLoadByTestcases(testcases, createLaunch({ environments: ['C', 'Y'] }));

            expect(actual).toStrictEqual(expected);
        });

        it('should return 0 when environments are empty', () => {
            const bookingApi = new BookingApiService({ debug, requests, devicesDownloader });

            const testcases = Array(5).fill(createTestCase(6596000));

            const expected = 0;

            const actual = bookingApi.calculateLoadByTestcases(testcases, createLaunch());

            expect(actual).toStrictEqual(expected);
        });
    });

    describe('getEnvironmentsMap', () => {
        it('should return object with environments', async() => {
            devicesDownloaderMock.mockResolvedValue([
                {
                    type: 'desktop',
                    environment: 'safari',
                    united_environment: 'Safari',
                },
            ]);

            requestsMock.mockResolvedValue({
                body: [
                    {
                        name: 'Safari',
                        code: '1',
                    },
                    {
                        name: 'GoogleChrome',
                        code: '2',
                    },
                ],
            });

            const bookingApi = new BookingApiService({ debug, requests, devicesDownloader });

            const launch = createLaunch({ environments: ['Safari', 'GoogleChrome'] });

            const expected = {
                Safari: {
                    ratio: 0.5,
                    launchEnvironment: 'Safari',
                    bookingEnvironmentName: 'Safari',
                    bookingEnvironmentCode: '1',
                },
                GoogleChrome: {
                    ratio: 0.5,
                    launchEnvironment: 'GoogleChrome',
                    bookingEnvironmentName: null,
                    bookingEnvironmentCode: null,
                },
            };

            const actual = await bookingApi.getEnvironmentsMap(launch);

            expect(devicesDownloaderMock).toHaveBeenCalled();
            expect(requestsMock).toHaveBeenCalled();
            expect(actual).toEqual(expected);
        });

        it('should throw BookingAssessorsEnvironmentsDownloadingError when failed to download environments', () => {
            devicesDownloaderMock.mockResolvedValue([]);

            requestsMock.mockRejectedValue(new Error('network error'));

            const bookingApi = new BookingApiService({ debug, requests, devicesDownloader });

            const launch = createLaunch();

            const expected = new BookingAssessorsEnvironmentsDownloadingError(new Error('network error'));

            const actual = bookingApi.getEnvironmentsMap(launch);

            return expect(actual).rejects.toThrowError(expected);
        });
    });

    describe('getPlatformsWithEnvironmentsFromProject', () => {
        it('should return an array with platforms and related environments', async() => {
            devicesDownloaderMock.mockResolvedValue([
                {
                    type: 'desktop',
                    environment: 'yabrowser',
                    united_environment: 'yabro',
                },
                {
                    type: 'touch-phone',
                    environment: 'ios_safari_7',
                    united_environment: 'Safari',
                },
                {
                    type: 'touch-phone',
                    environment: 'ios_Safari_11',
                    united_environment: 'Safari',
                },
                {
                    type: 'tv',
                    environment: 'tizen4',
                    united_environment: 'tizen',
                },
            ]);

            const bookingApi = new BookingApiService({ debug, requests, devicesDownloader });

            const project = {
                settings: {
                    environments: [
                        { title: 'iOS_safari_11' },
                        { title: 'iOS_safari_7' },
                        { title: 'yabrowser' },
                    ],
                },
            } as unknown as Project;

            const expected = [
                { platform: 'desktop', environments: ['yabrowser'] },
                { platform: 'touch-phone', environments: ['ios_safari_7', 'ios_Safari_11'] },
            ];

            const actual = await bookingApi.getPlatformsWithEnvironmentsFromProject(project);

            expect(actual).toEqual(expected);
        });
    });
});
