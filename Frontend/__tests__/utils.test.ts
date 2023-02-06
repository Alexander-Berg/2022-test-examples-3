import { getStreamMediaState } from '../utils';

describe('Utils', () => {
    describe('#getStreamMediaState', () => {
        let stream;
        let audioTracks;
        let videoTracks;

        beforeEach(() => {
            videoTracks = [{ kind: 'audio', enabled: false }];
            audioTracks = [{ kind: 'video', enabled: false }];

            stream = {
                getVideoTracks() {
                    return videoTracks;
                },

                getAudioTracks() {
                    return audioTracks;
                },
            };
        });

        test('Return correct result if no stream provided', () => {
            expect(getStreamMediaState()).toStrictEqual({
                video: false,
                audio: false,
            });
        });

        test('Returns correct result if video and audio tracks disabled', () => {
            // @ts-ignore
            expect(getStreamMediaState(stream)).toStrictEqual({
                video: false,
                audio: false,
            });
        });

        test('Return correct result if video track enabled', () => {
            videoTracks[0].enabled = true;

            // @ts-ignore
            expect(getStreamMediaState(stream)).toStrictEqual({
                video: true,
                audio: false,
            });
        });

        test('Return correct result if audio track enabled', () => {
            audioTracks[0].enabled = true;

            // @ts-ignore
            expect(getStreamMediaState(stream)).toStrictEqual({
                video: false,
                audio: true,
            });
        });

        test('Returns correct result if video and audio tracks enabled', () => {
            videoTracks[0].enabled = true;
            audioTracks[0].enabled = true;

            // @ts-ignore
            expect(getStreamMediaState(stream)).toStrictEqual({
                video: true,
                audio: true,
            });
        });
    });
});
