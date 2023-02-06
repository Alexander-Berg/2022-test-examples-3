/* eslint-disable */
import { useState, useReducer, useCallback, useEffect } from 'react';
import { useAudio } from 'react-use';
import nanoid from 'nanoid/non-secure';
import { audioPlayerReducer, initialAudioPlayerState } from '../../reducers/testChat/audioPlayer';
import {
    AudioPlaybackState,
    AudioPlayerActionPlayData,
    TestAudioTrack,
    AudioPlayerActionData,
    AudioPlayerAction,
    AudioPlayerActionStopData,
    AudioPlayerEventType,
    AudioPlayerPlayBehaviour,
} from '../../types/skillTest';
import { useOneTimeCallback } from '../useOneTimeCallback';

enum AudioPlayerErrorType {
    MEDIA_ERROR_SERVICE_UNAVAILABLE = 'MEDIA_ERROR_SERVICE_UNAVAILABLE',
    MEDIA_ERROR_UNKNOWN = 'MEDIA_ERROR_UNKNOWN',
}

interface UseTestAudioPlayerParams {
    hooks: {
        onPlay: () => void;
        onPause: () => void;
        onFinish: () => void;
        onNearlyFinished: () => void;
        onError: (type: AudioPlayerErrorType, message: string) => void;
    };
}

export const useTestAudioPlayer = (params: UseTestAudioPlayerParams) => {
    const [audioPlayerState, dispatchAudioPlayerAction] = useReducer(audioPlayerReducer, initialAudioPlayerState);
    const [playbackState, setPlaybackState] = useState<AudioPlaybackState | null>(null);
    const [isAudioMetaLoading, setIsAudioMetaLoading] = useState(false);
    const { currentTrack, queue } = audioPlayerState;
    const { hooks } = params;

    const onPlaying: React.ReactEventHandler<HTMLAudioElement> = useCallback(
        e => {
            if (!currentTrack) {
                return;
            }

            setPlaybackState(AudioPlaybackState.PLAYING);
            hooks.onPlay();
        },
        [currentTrack, hooks],
    );

    const onPause: React.ReactEventHandler<HTMLAudioElement> = useCallback(
        e => {
            if (currentTrack === null) {
                return;
            }

            const audio = e.currentTarget;

            if (audio.currentTime !== audio.duration && audio.error === null) {
                setPlaybackState(AudioPlaybackState.STOPPED);
                hooks.onPause();
            }
        },
        [currentTrack, hooks],
    );

    const onEnded: React.ReactEventHandler<HTMLAudioElement> = useCallback(
        e => {
            if (!currentTrack) {
                return;
            }

            setPlaybackState(AudioPlaybackState.FINISHED);

            if (queue.length > 0) {
                dispatchAudioPlayerAction({ type: 'PlayNext' });
                hooks.onFinish();

                return;
            }

            hooks.onFinish();
        },
        [currentTrack, hooks],
    );

    const onLoadStart: React.ReactEventHandler<HTMLAudioElement> = useCallback(
        e => {
            if (currentTrack !== null && !isAudioMetaLoading) {
                setIsAudioMetaLoading(true);
            }
        },
        [currentTrack, isAudioMetaLoading],
    );

    const onLoadedMetadata: React.ReactEventHandler<HTMLAudioElement> = useCallback(
        e => {
            if (currentTrack !== null) {
                setIsAudioMetaLoading(false);
            }
        },
        [currentTrack, isAudioMetaLoading],
    );

    const onNearlyFinished = useOneTimeCallback(hooks.onNearlyFinished, currentTrack?.id ?? 'default');

    const onTimeUpdate: React.ReactEventHandler<HTMLAudioElement> = useCallback(
        e => {
            const { currentTime, duration } = e.currentTarget;
            const isNearlyFinished = currentTime / duration > 0.66;

            if (currentTrack !== null && isNearlyFinished && playbackState === AudioPlaybackState.PLAYING) {
                onNearlyFinished();
            }
        },
        [currentTrack, onNearlyFinished, playbackState],
    );

    const onError: React.ReactEventHandler<HTMLAudioElement> = useCallback(
        e => {
            if (currentTrack !== null) {
                setIsAudioMetaLoading(false);

                const error = e.currentTarget.error!;

                const errorMessage = `${error.code}${error.message ? ` ${error.message}` : ''}`;

                switch (error.code) {
                    case MediaError.MEDIA_ERR_NETWORK:
                    case MediaError.MEDIA_ERR_DECODE:
                    case MediaError.MEDIA_ERR_SRC_NOT_SUPPORTED:
                        hooks.onError(AudioPlayerErrorType.MEDIA_ERROR_SERVICE_UNAVAILABLE, errorMessage);

                        break;

                    default:
                        hooks.onError(AudioPlayerErrorType.MEDIA_ERROR_UNKNOWN, errorMessage);
                }
            }
        },
        [currentTrack],
    );

    const [audioEl, audioState, audioControls, audioRef] = useAudio({
        src: currentTrack?.url ?? '',
        hidden: true,
        preload: 'auto',
        onPlaying,
        onPause,
        onEnded,
        onLoadStart,
        onLoadedMetadata,
        onTimeUpdate,
        onError,
    });

    const playAudio = useCallback((from?: number) => {
        if (audioRef.current) {
            if (!audioRef.current.paused) {
                audioRef.current.pause();
            }

            audioRef.current.currentTime = from ?? 0;

            void audioRef.current.play();
        }
    }, []);

    useEffect(() => {
        if (currentTrack !== null) {
            playAudio(currentTrack.offsetSec);
        }
    }, [currentTrack]);

    useEffect(() => {
        if (playbackState === AudioPlaybackState.FINISHED && queue.length > 0) {
            dispatchAudioPlayerAction({ type: 'PlayNext' });
        }
    }, [queue, playbackState]);

    const onAudioPlayResponse = useCallback(
        (data: AudioPlayerActionPlayData, toEvent?: AudioPlayerEventType) => {
            const { item } = data;
            const { offset_ms, token, url, expected_previous_token } = item.stream;
            const offsetSec = offset_ms / 1000;

            const { title, art } = item.metadata ?? {};

            const behavior: AudioPlayerPlayBehaviour =
                toEvent === AudioPlayerEventType.PlaybackNearlyFinished ?
                    AudioPlayerPlayBehaviour.ENQUEUE :
                    AudioPlayerPlayBehaviour.REPLACE_ALL;

            const track: TestAudioTrack = {
                id: nanoid(),
                title,
                logoUrl: art?.url,
                expectedPreviousToken: expected_previous_token,
                token,
                url,
                offsetSec,
            };

            dispatchAudioPlayerAction({
                type: 'Play',
                payload: {
                    behavior,
                    track,
                    checkPreviousToken: __appConfig.skillTesting.audioPlayer.checkPreviousToken,
                },
            });
        },
        [playbackState],
    );

    const onAudioStopResponse = useCallback(
        (data: AudioPlayerActionStopData) => {
            audioControls.pause();
        },
        [audioControls],
    );

    const onAudioResponse = useCallback(
        (audioPlayerActionData: AudioPlayerActionData, toEvent?: AudioPlayerEventType) => {
            switch (audioPlayerActionData.action) {
                case AudioPlayerAction.Play:
                    onAudioPlayResponse(audioPlayerActionData, toEvent);

                    break;

                case AudioPlayerAction.Stop:
                    onAudioStopResponse(audioPlayerActionData);

                    break;
            }
        },
        [onAudioPlayResponse, onAudioStopResponse],
    );

    const init = useCallback(() => {
        dispatchAudioPlayerAction({ type: 'Init' });
    }, []);

    const playNext = useCallback(() => {
        dispatchAudioPlayerAction({ type: 'PlayNext' });
    }, []);

    return {
        audioEl,
        currentTrack,
        queue,
        onAudioResponse,
        init,
        currentTime: audioState.time,
        duration: audioState.duration,
        playbackState,
        isAudioMetaLoading,
        audioControls,
        playNext,
    };
};
