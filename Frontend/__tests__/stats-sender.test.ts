import { StatsSender } from '../stats-sender';

const mediator = {
    keepAlive: jest.fn(),
};

const connection = {
    getStats: jest.fn(() => Promise.resolve([
        {
            id: 1,
            type: 'transport',
            timestamp: 5,
            bytesSent: 1000,
            bytesReceived: 2000,
            dtlsState: 'new',
            selectedCandidatePairId: 2,
            localCertificateId: 8,
            remoteCertificateId: 8,
        },
        {
            id: 2,
            totalRoundTripTime: 100,
            currentRoundTripTime: 200,
            responsesReceived: 300,
            nominated: 400,
            bytesSent: 500,
            bytesReceived: 600,
            localCandidateId: 3,
            remoteCandidateId: 4,
            availableIncomingBitrate: 700,
            availableOutgoingBitrate: 800,
        },
        {
            id: 3,
            ip: 'local-ip',
            port: 3000,
            protocol: 'upd',
            candidateType: 'relay',
            networkType: 'wifi',
            relayProtocol: 'upd',
        },
        {
            id: 4,
            ip: 'remote-ip',
            port: 4000,
            protocol: 'upd',
            candidateType: 'relay',
            networkType: 'wifi',
            relayProtocol: 'upd',
        },
        {
            id: 5,
            mimeType: 'mp4',
            clockRate: 100,
            payloadType: 'video',
        },
        {
            id: 6,
            trackIdentifier: 'audio-track-id',
            concealedSamples: 100,
            concealmentEvents: 200,
            totalSamplesReceived: 300,
            totalSamplesDuration: 400,
            audioLevel: 500,
            totalAudioEnergy: 600,
        },
        {
            id: 7,
            trackIdentifier: 'video-track-id',
            frameWidth: 100,
            frameHeight: 200,
            framesReceived: 300,
            framesSent: 400,
            framesDropped: 500,
            partialFramesLost: 600,
            fullFramesLost: 700,
        },
        {
            id: 8,
            fingerprint: 'certificate-fingerprint',
        },
        {
            type: 'inbound-rtp',
            kind: 'video',
            packetsReceived: 100,
            packetsLost: 200,
            framesDecoded: 300,
            nackCount: 400,
            codecId: 5,
            bytesReceived: 500,
            fractionLost: 600,
            pliCount: 700,
            trackId: 7,
        },
        {
            type: 'inbound-rtp',
            kind: 'audio',
            packetsReceived: 100,
            packetsLost: 200,
            jitter: 300,
            fractionLost: 400,
            trackId: 6,
        },
        {
            type: 'outbound-rtp',
            kind: 'video',
            bytesSent: 100,
            packetsSent: 200,
            firCount: 300,
            framesEncoded: 400,
            nackCount: 500,
            pliCount: 600,
            codecId: 5,
            trackId: 7,
        },
        {
            type: 'outbound-rtp',
            kind: 'audio',
            bytesSent: 100,
            packetsSent: 200,
            codecId: 5,
            trackId: 6,
        },
    ])),
};

describe('StatsSender', () => {
    let statsSender;

    beforeAll(() => {
        jest.useFakeTimers();
    });

    beforeEach(() => {
        statsSender = new StatsSender({
            guid: 'test-guid',
            // @ts-ignore
            mediator,
            // @ts-ignore
            connection,
        });
    });

    describe('#start', () => {
        test('Clear interval', () => {
            statsSender.start();

            expect(clearInterval).toBeCalled();
        });

        test('Set new interval', () => {
            statsSender.start();

            expect(setInterval).toBeCalledWith(statsSender.handleInterval, 10000);
        });
    });

    describe('#handleInterval', () => {
        beforeEach(() => {
            statsSender.getStatsReport = jest.fn(() => Promise.resolve('stats'));
        });

        test('Send mediator keep alive', (done) => {
            statsSender
                .handleInterval()
                .then(() => {
                    expect(mediator.keepAlive).toBeCalledWith('stats');
                    done();
                });
        });

        test('Emit stats event', (done) => {
            statsSender.onStats.addListener((stats) => {
                expect(stats).toStrictEqual('stats');
                done();
            });

            statsSender.handleInterval();
        });

        test('Emit error if promise rejected', (done) => {
            statsSender.getStatsReport = jest.fn(() => Promise.reject('Error'));

            statsSender.onError.addListener((error) => {
                expect(error).toStrictEqual('Error');
                done();
            });

            statsSender.handleInterval();
        });
    });

    describe('#getStatsReport', () => {
        test('Return correct value in promise', () => {
            expect(statsSender.getStatsReport()).resolves.toStrictEqual({
                standard: {
                    timestamp: 5000,
                    transport: {
                        bytes_sent: 1000,
                        bytes_received: 2000,
                        dtls_state: 'new',
                        selected_candidate_pair: {
                            total_round_trip_time: 100,
                            current_round_trip_time: 200,
                            responses_received: 300,
                            nominated: 400,
                            bytes_sent: 500,
                            bytes_received: 600,
                            local_candidate: {
                                candidate_type: 'relay',
                                ip: 'local-ip',
                                port: 3000,
                                protocol: 'upd',
                                network_type: 'wifi',
                                relay_protocol: 'upd',
                            },
                            remote_candidate: {
                                candidate_type: 'relay',
                                ip: 'remote-ip',
                                port: 4000,
                                protocol: 'upd',
                                network_type: 'wifi',
                                relay_protocol: 'upd',
                            },
                            available_incoming_bitrate: 700,
                            available_outgoing_bitrate: 800,
                        },
                        local_certificate: {
                            fingerprint: 'certificate-fingerprint',
                        },
                        remote_certificate: {
                            fingerprint: 'certificate-fingerprint',
                        },
                    },
                    inbound_rtp_video_stream: {
                        packets_received: 100,
                        packets_lost: 200,
                        frames_decoded: 300,
                        nack_count: 400,
                        bytes_received: 500,
                        fraction_lost: 600,
                        pli_count: 700,
                        codec: {
                            clock_rate: 100,
                            mime_type: 'mp4',
                            payload_type: 'video',
                        },
                        video_track: {
                            frame_height: 200,
                            frame_width: 100,
                            frames_dropped: 500,
                            frames_received: 300,
                            frames_sent: 400,
                            full_frames_lost: 700,
                            partial_frames_lost: 600,
                            track_identifier: 'video-track-id',
                        },
                    },
                    inbound_rtp_audio_stream: {
                        jitter: 300,
                        packets_lost: 200,
                        packets_received: 100,
                        fraction_lost: 400,
                        audio_track: {
                            track_identifier: 'audio-track-id',
                            concealed_samples: 100,
                            concealment_events: 200,
                            total_samples_received: 300,
                            total_samples_duration: 400,
                            audio_level: 500,
                            total_audio_energy: 600,
                        },
                    },
                    outbound_rtp_video_stream: {
                        bytes_sent: 100,
                        frames_encoded: 400,
                        nack_count: 500,
                        packets_sent: 200,
                        codec: {
                            clock_rate: 100,
                            mime_type: 'mp4',
                            payload_type: 'video',
                        },
                        video_track: {
                            frame_height: 200,
                            frame_width: 100,
                            frames_dropped: 500,
                            frames_received: 300,
                            frames_sent: 400,
                            full_frames_lost: 700,
                            partial_frames_lost: 600,
                            track_identifier: 'video-track-id',
                        },
                    },
                    outbound_rtp_audio_stream: {
                        bytes_sent: 100,
                        packets_sent: 200,
                        codec: {
                            clock_rate: 100,
                            mime_type: 'mp4',
                            payload_type: 'video',
                        },
                        audio_track: {
                            track_identifier: 'audio-track-id',
                            concealed_samples: 100,
                            concealment_events: 200,
                            total_samples_received: 300,
                            total_samples_duration: 400,
                            audio_level: 500,
                            total_audio_energy: 600,
                        },
                    },
                },
            });
        });
    });
});
