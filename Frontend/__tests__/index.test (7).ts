import { MediaProvider } from '..';
import MediaStreamMock from '../__mocks__/media-stream';
import MediaDevicesMock from '../__mocks__/media-devices';

describe('MediaProvider', () => {
    let mediaProvider: MediaProvider;

    beforeAll(() => {
        // @ts-ignore
        global.MediaStream = MediaStreamMock;

        // @ts-ignore
        global.navigator.mediaDevices = MediaDevicesMock;

        mediaProvider = new MediaProvider();
    });

    beforeEach(() => {
        jest.clearAllMocks();
    });

    describe('#toggleDisplay', () => {
        const videoTrack = {
            kind: 'video',
            stop: () => {},
        };

        beforeEach(() => {
            mediaProvider.loadStream = jest.fn();

            mediaProvider.setCallParams({
                displayEnabled: false,
                displayAvailable: true,
            });

            (mediaProvider as any).stream.getTracks = jest.fn(() => [videoTrack]);
        });

        it('Change displayEnabled value', () => {
            mediaProvider.toggleDisplay();

            expect(mediaProvider).toBeTruthy();
        });

        it('Load stream', () => {
            mediaProvider.toggleDisplay();

            expect(mediaProvider.loadStream).toBeCalled();
        });

        it('Not load stream if display is unavailable', () => {
            mediaProvider.setCallParams({
                displayAvailable: false,
            });

            mediaProvider.toggleDisplay();

            expect(mediaProvider.loadStream).not.toBeCalled();
        });

        it('Remove track from stream', () => {
            mediaProvider.toggleDisplay();

            expect((mediaProvider as any).stream.removeTrack).toBeCalledWith(videoTrack);
        });

        it('Not remove track from stream if display is unavailable', () => {
            mediaProvider.setCallParams({
                displayAvailable: false,
            });

            mediaProvider.toggleDisplay();

            expect((mediaProvider as any).stream.removeTrack).not.toBeCalled();
        });
    });

    describe('#getStream', () => {
        it('Call getUserMedia with correct constraints', async () => {
            (mediaProvider as any).stream.getVideoTracks = jest.fn(() => []);

            mediaProvider.setCallParams({
                audioEnabled: true,
                videoEnabled: true,
            });

            mediaProvider.setConstraints({
                video: {
                    width: 1920,
                    height: 1080,
                },
                audio: {},
            });

            await (mediaProvider as any).getStream();

            expect(MediaDevicesMock.getUserMedia).toBeCalledWith({
                video: {
                    deviceId: undefined,
                    width: 1920,
                    height: 1080,
                },
                audio: {
                    deviceId: undefined,
                },
            });
        });

        it('Call getUserMedia with correct constraints if devices is set', async () => {
            mediaProvider.setConstraints({
                video: {},
                audio: {},
            });

            mediaProvider.setCallParams({
                audioEnabled: true,
                videoEnabled: true,
            });

            mediaProvider.setVideoInput({
                deviceId: 'video-device-id',
            } as any);

            mediaProvider.setAudioInput({
                deviceId: 'audio-device-id',
            } as any);

            await (mediaProvider as any).getStream();

            expect(MediaDevicesMock.getUserMedia).toBeCalledWith({
                video: {
                    deviceId: {
                        exact: 'video-device-id',
                    },
                },
                audio: {
                    deviceId: {
                        exact: 'audio-device-id',
                    },
                },
            });
        });

        it('Call getDisplayMedia if display enabled', async () => {
            mediaProvider.setCallParams({
                displayEnabled: true,
            });

            await (mediaProvider as any).getStream();

            expect(MediaDevicesMock.getDisplayMedia).toBeCalledWith({
                video: true,
            });
        });

        it('Set videoEnabled=true if display media stream available', async () => {
            mediaProvider.setCallParams({
                videoEnabled: false,
                displayEnabled: true,
            });

            await (mediaProvider as any).getStream();

            expect((mediaProvider as any).callParams.videoEnabled).toBeTruthy();
        });

        it('Call getUserMedia if media devices return error', async () => {
            MediaDevicesMock.getDisplayMedia = jest.fn(() => Promise.reject());

            mediaProvider.setCallParams({
                displayEnabled: true,
            });

            (mediaProvider as any).isDesktop = true;

            await (mediaProvider as any).getStream();

            expect(MediaDevicesMock.getUserMedia).toBeCalled();
        });
    });
});
