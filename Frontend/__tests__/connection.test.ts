import { RTCConnection, RTCCodecType } from '../connection';
import RTCPeerConnectionMock from '../__mocks__/peer-connection';

describe('RTCConnection', () => {
    let connection;

    beforeAll(() => {
        // @ts-ignore
        global.RTCPeerConnection = RTCPeerConnectionMock;
    });

    beforeEach(() => {
        connection = new RTCConnection();
    });

    describe('#constructor', () => {
        test('Set default candidates gathering timeout', () => {
            expect(connection.candidatesGatheringTimeout).toStrictEqual(100);
        });

        test('Set candidates gathering timeout from params', () => {
            connection = new RTCConnection({ candidatesGatheringTimeout: 300 });

            expect(connection.candidatesGatheringTimeout).toStrictEqual(300);
        });
    });

    describe('#open', () => {
        test('Subscribe to RTCPeerConnection events', () => {
            connection.open({});

            expect(connection.peerConnection.addEventListener).toBeCalledWith('icecandidate', expect.any(Function));
            expect(connection.peerConnection.addEventListener).toBeCalledWith('track', expect.any(Function));
            expect(connection.peerConnection.addEventListener).toBeCalledWith('iceconnectionstatechange', expect.any(Function));
            expect(connection.peerConnection.addEventListener).toBeCalledWith('negotiationneeded', expect.any(Function));
        });
    });

    describe('#update', () => {
        it('Set configuration with correct params', () => {
            connection.peerConnection.getConfiguration = jest.fn(() => ({
                iceServers: 'old-ice-servers',
                sdpSemantics: 'unified-plan',
            }));

            connection.update({
                iceServers: 'new-ice-servers',
            });

            expect(connection.peerConnection.setConfiguration).toBeCalledWith({
                iceServers: 'new-ice-servers',
                sdpSemantics: 'unified-plan',
            });
        });
    });

    describe('#setOffer', () => {
        test('Call RTCPeerConnection setRemoteDescription method with correct args', () => {
            connection.setOffer('test-offer');

            expect(connection.peerConnection.setRemoteDescription).toBeCalledWith({
                type: 'offer',
                sdp: 'test-offer',
            });
        });
    });

    describe('#createOffer', () => {
        test('Create offer with correct params', () => {
            connection.createOffer('params');

            expect(connection.peerConnection.createOffer).toBeCalledWith('params');
        });

        test('Save description to offer variable', () => {
            return connection
                .createOffer('params')
                .then(() => {
                    expect(connection.offer).toEqual('offerDescription');
                });
        });

        test('Return description in promise', () => {
            expect(connection.createOffer('params')).resolves.toEqual('offerDescription');
        });
    });

    describe('#createAnswer', () => {
        test('Create RTCPeerConnection answer', () => {
            connection.createAnswer('params');

            expect(connection.peerConnection.createAnswer).toBeCalledWith('params');
        });

        test('Set local description with correct value', () => {
            return connection
                .createAnswer('params')
                .then(() => {
                    expect(connection.peerConnection.setLocalDescription).toBeCalledWith('answerDescription');
                });
        });

        test('Return promise with correct value', () => {
            expect(connection.createAnswer('params')).resolves.toEqual('answerDescription');
        });
    });

    describe('#setAnswer', () => {
        test('Set local description if offer exists', () => {
            connection.offer = 'offer';

            return connection
                .setAnswer('answer')
                .then(() => {
                    expect(connection.peerConnection.setLocalDescription).toBeCalledWith('offer');
                });
        });

        test('Not set local description if offer not exists', () => {
            return connection
                .setAnswer('answer')
                .then(() => {
                    expect(connection.peerConnection.setLocalDescription).not.toBeCalled();
                });
        });

        test('Set remote description with correct value', () => {
            return connection
                .setAnswer('answer')
                .then(() => {
                    expect(connection.peerConnection.setRemoteDescription).toBeCalledWith({
                        type: 'answer',
                        sdp: 'answer',
                    });
                });
        });
    });

    describe('#addTrack', () => {
        let track;
        let transceiver;
        let sender;

        beforeEach(() => {
            track = {
                kind: 'video',
                stop: jest.fn(),
            };

            transceiver = {
                sender: {
                    track,
                },
                setCodecPreferences: jest.fn(),
            };

            sender = {
                track,
                replaceTrack: jest.fn(),
            };

            connection.peerConnection.getTransceivers = jest.fn(() => [transceiver]);
            connection.peerConnection.getSenders = jest.fn(() => []);

            connection.isUnifiedPlan = jest.fn(() => true);

            connection.setVideoCodecs([{
                name: 'H264',
                params: {
                    clockRate: '60000',
                    sdpFmtpLine: 'test-param=1',
                },
            }]);
        });

        test('Add track to RTCPeerConnection if sender does not exist', () => {
            connection.addTrack(track, 'test-stream');

            expect(connection.peerConnection.addTrack).toBeCalledWith(track, 'test-stream');
        });

        test('Not add track to RTCPeerConnection if sender exists', () => {
            connection.peerConnection.getSenders = jest.fn(() => [sender]);

            connection.addTrack(track, 'test-stream');

            expect(connection.peerConnection.addTrack).not.toBeCalled();
        });

        test('Set correct codec preferences for video transceiver', () => {
            connection.addTrack(track, 'test-stream');

            expect(transceiver.setCodecPreferences).toBeCalledWith([{
                mimeType: 'video/H264',
                clockRate: 60000,
                sdpFmtpLine: 'test-param=1',
            }]);
        });

        test('Not set codecs for audio track', () => {
            track.kind = 'audio';

            connection.addTrack(track, 'test-stream');

            expect(transceiver.setCodecPreferences).not.toBeCalled();
        });

        test('Not set codecs if not unified plan', () => {
            connection.isUnifiedPlan = jest.fn(() => false);

            connection.addTrack(track, 'test-stream');

            expect(transceiver.setCodecPreferences).not.toBeCalled();
        });

        test('Not set codecs if no video codecs', () => {
            connection.setVideoCodecs(undefined);

            connection.addTrack(track, 'test-stream');

            expect(transceiver.setCodecPreferences).not.toBeCalled();
        });

        test('Stop track if sender track is another', () => {
            connection.peerConnection.getSenders = jest.fn(() => [sender]);

            connection.addTrack({ kind: 'video' }, 'test-stream');

            expect(track.stop).toBeCalled();
        });

        test('Replace track if sender track is another', () => {
            connection.peerConnection.getSenders = jest.fn(() => [sender]);

            const newTrack = { kind: 'video' };

            connection.addTrack(newTrack, 'test-stream');

            expect(sender.replaceTrack).toBeCalledWith(newTrack);
        });
    });

    describe('#addStream', () => {
        test('Add all tracks from stream', () => {
            const stream = {
                getTracks() {
                    return ['test-track-1', 'test-track-2'];
                },
            };

            connection.addTrack = jest.fn();
            connection.addStream(stream);

            expect(connection.addTrack).toBeCalledWith('test-track-1', stream);
            expect(connection.addTrack).toBeCalledWith('test-track-2', stream);
        });
    });

    describe('#handlePeerIceCandidate', () => {
        beforeAll(() => {
            jest.useFakeTimers();
        });

        test('Cache candidate', () => {
            connection.handlePeerIceCandidate({ candidate: 'test-candidate' });

            expect(connection.candidatesCache).toContain('test-candidate');
        });

        test('Emit iceCandidate event with correct data', (done) => {
            connection.onIceCandidate.addListener((candidate) => {
                expect(candidate).toStrictEqual('test-candidate');
                done();
            });

            connection.handlePeerIceCandidate({ candidate: 'test-candidate' });
        });

        test('Clear timeout', () => {
            connection.handlePeerIceCandidate({ candidate: 'test-candidate' });

            expect(clearTimeout).toBeCalled();
        });

        test('Create timeout with correct params', () => {
            connection.handlePeerIceCandidate({ candidate: 'test-candidate' });

            expect(setTimeout).toBeCalledWith(connection.handleCandidatesGathered, 100);
        });

        test('Emit candidates after timeout', (done) => {
            connection.onIceCandidates.addListener((candidates) => {
                expect(candidates).toEqual(expect.arrayContaining(['test-candidate-1', 'test-candidate-2']));
                done();
            });

            connection.handlePeerIceCandidate({ candidate: 'test-candidate-1' });
            connection.handlePeerIceCandidate({ candidate: 'test-candidate-2' });

            jest.advanceTimersByTime(500);
        });
    });

    describe('#handlePeerTrack', () => {
        let event;

        beforeEach(() => {
            event = {
                streams: [{ id: 'test-stream' }],
            };
        });

        test('Save stream to variable', () => {
            connection.currentStream = { id: 'another-stream' };

            connection.handlePeerTrack(event);

            expect(connection.currentStream).toStrictEqual(event.streams[0]);
        });

        test('Emit stream', (done) => {
            connection.onStream.addListener((stream) => {
                expect(stream).toStrictEqual(event.streams[0]);
                done();
            });

            connection.handlePeerTrack(event);
        });

        test('Not emit current stream', () => {
            const streamMock = jest.fn();

            connection.currentStream = { id: 'test-stream' };

            connection.handlePeerTrack(event);

            expect(streamMock).not.toBeCalled();
        });
    });

    describe('#isUnifiedPlan', () => {
        beforeEach(() => {
            // @ts-ignore
            Object.defineProperty(global.window.navigator, 'userAgent', {
                value: 'Mozilla/5.0 AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.77 Safari/537.36',
                configurable: true,
            });
        });

        test('Returns true if browser is firefox', () => {
            // @ts-ignore
            Object.defineProperty(global.window.navigator, 'userAgent', {
                value: 'Mozilla/5.0 (Windows NT 6.1; WOW64; rv:64.0) Gecko/20100101 Firefox/64.0',
            });

            expect(connection.isUnifiedPlan()).toBeTruthy();
        });

        test('Returns true if config sdpSemantics is unified-plan', () => {
            connection.peerConnection.getConfiguration = jest.fn(() => ({
                sdpSemantics: 'unified-plan',
            }));

            expect(connection.isUnifiedPlan()).toBeTruthy();
        });

        test('Returns false if not unified plan', () => {
            expect(connection.isUnifiedPlan()).toBeFalsy();
        });
    });

    describe('#getSupportedCodecs', () => {
        beforeEach(() => {
            Object.defineProperty(global, 'RTCRtpSender', {
                value: {
                    getCapabilities: jest.fn(),
                },
                configurable: true,
            });

            Object.defineProperty(global, 'RTCRtpReceiver', {
                value: {
                    getCapabilities: jest.fn(),
                },
                configurable: true,
            });
        });

        it('Returns blank array if RTCRtpSender getCapabilities returns undefined', () => {
            expect(RTCConnection.getSupportedCodecs(RTCCodecType.ENCODER)).toStrictEqual([]);
        });

        it('Returns blank array if RTCRtpReceiver getCapabilities returns undefined', () => {
            expect(RTCConnection.getSupportedCodecs(RTCCodecType.DECODER)).toStrictEqual([]);
        });

        // https://st.yandex-team.ru/MSSNGRFRONT-6200
        it.skip('Returns correct codecs', () => {
            Object.defineProperty(global, 'RTCRtpSender', {
                value: {
                    getCapabilities: jest.fn(() => ({
                        codecs: [
                            {
                                mimeType: 'video/H264',
                                clockRate: 60000,
                                sdpFmtpLine: 'test-param=1',
                            },
                            {
                                mimeType: 'video/VP8',
                                clockRate: 50000,
                            },
                        ],
                    })),
                },
            });

            expect(RTCConnection.getSupportedCodecs(RTCCodecType.ENCODER)).toStrictEqual([
                {
                    name: 'H264',
                    params: {
                        clockRate: '60000',
                        sdpFmtpLine: 'test-param=1',
                    },
                },
                {
                    name: 'VP8',
                    params: {
                        clockRate: '50000',
                        sdpFmtpLine: undefined,
                    },
                },
            ]);
        });
    });
});
