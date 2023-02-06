/* eslint-disable */
import { audioPlayerReducer, initialAudioPlayerState, AudioPlayerState } from './audioPlayer';
import { AudioPlayerPlayBehaviour, TestAudioTrack } from '../../types/skillTest';

const testTrack1: TestAudioTrack = {
    id: '1',
    offsetSec: 0,
    token: 'token1',
    url: 'https://example.com',
};

const testTrack2: TestAudioTrack = {
    id: '2',
    offsetSec: 0,
    token: 'token2',
    url: 'https://example.com',
    expectedPreviousToken: 'token1',
};

const testTrack3: TestAudioTrack = {
    id: '3',
    offsetSec: 0,
    token: 'token3',
    url: 'https://example.com',
    expectedPreviousToken: 'token2',
};

const testTrack4: TestAudioTrack = {
    id: '4',
    offsetSec: 0,
    token: 'token2',
    url: 'https://example.com',
    expectedPreviousToken: 'token2',
};

const checkPreviousToken = true;

describe('audioPlayerReducer', () => {
    it('sould handle play action enqueue when audio is not playing', () => {
        const newState = audioPlayerReducer(initialAudioPlayerState, {
            type: 'Play',
            payload: {
                behavior: AudioPlayerPlayBehaviour.ENQUEUE,
                track: testTrack1,
                checkPreviousToken,
            },
        });

        expect(newState).toEqual({
            ...initialAudioPlayerState,
            currentTrack: testTrack1,
            queue: [],
        });
    });

    it('sould handle play action enqueue when expectedPreviousToken mismatches and checkOption disabled', () => {
        const state: AudioPlayerState = {
            currentTrack: testTrack1,
            queue: [],
        };

        const newState = audioPlayerReducer(state, {
            type: 'Play',
            payload: {
                behavior: AudioPlayerPlayBehaviour.ENQUEUE,
                track: testTrack3,
                checkPreviousToken: false,
            },
        });

        expect(newState).toEqual({
            ...initialAudioPlayerState,
            currentTrack: testTrack1,
            queue: [testTrack3],
        });
    });

    it('sould handle play action enqueue when expectedPreviousToken matches', () => {
        const state: AudioPlayerState = {
            currentTrack: testTrack1,
            queue: [],
        };

        const newState = audioPlayerReducer(state, {
            type: 'Play',
            payload: {
                behavior: AudioPlayerPlayBehaviour.ENQUEUE,
                track: testTrack2,
                checkPreviousToken,
            },
        });

        expect(newState).toEqual({
            ...initialAudioPlayerState,
            currentTrack: testTrack1,
            queue: [testTrack2],
        });
    });

    it('sould handle play action and replace current queue', () => {
        const state: AudioPlayerState = {
            currentTrack: testTrack2,
            queue: [testTrack3],
        };

        const newState = audioPlayerReducer(state, {
            type: 'Play',
            payload: {
                behavior: AudioPlayerPlayBehaviour.ENQUEUE,
                track: testTrack4,
                checkPreviousToken,
            },
        });

        expect(newState).toEqual({
            ...initialAudioPlayerState,
            currentTrack: testTrack2,
            queue: [testTrack4],
        });
    });

    it('sould handle play action replace all', () => {
        const newState = audioPlayerReducer(initialAudioPlayerState, {
            type: 'Play',
            payload: {
                behavior: AudioPlayerPlayBehaviour.REPLACE_ALL,
                track: testTrack1,
            },
        });

        expect(newState).toEqual({
            ...initialAudioPlayerState,
            currentTrack: testTrack1,
            queue: [],
        });
    });

    const clearQueueTestState: AudioPlayerState = {
        currentTrack: testTrack1,
        queue: [testTrack2],
    };

    it('sould handle play next action', () => {
        const state: AudioPlayerState = {
            currentTrack: testTrack1,
            queue: [testTrack2],
        };

        const newState = audioPlayerReducer(state, { type: 'PlayNext' });

        expect(newState).toEqual({
            currentTrack: testTrack2,
            queue: [],
        });
    });

    it('sould handle play next action with empty queue', () => {
        const state: AudioPlayerState = {
            currentTrack: testTrack1,
            queue: [],
        };

        const newState = audioPlayerReducer(state, { type: 'PlayNext' });

        expect(newState).toEqual({
            currentTrack: testTrack1,
            queue: [],
        });
    });
});
