/* eslint-disable */
import { TestAudioTrack, AudioPlayerPlayBehaviour, AudioPlayerClearQueueBehaviour } from '../../types/skillTest';
import { ReducerAction } from '../../model/reducer';

export interface AudioPlayerState {
    currentTrack: TestAudioTrack | null;
    queue: TestAudioTrack[];
}

export const initialAudioPlayerState: AudioPlayerState = {
    currentTrack: null,
    queue: [],
};

type AudioPlayerActionType = 'Play' | 'PlayNext' | 'ClearQueue' | 'Init';

interface AudioPlayerPayload extends Record<AudioPlayerActionType, any> {
    Play: {
        track: TestAudioTrack;
        behavior: AudioPlayerPlayBehaviour;
        checkPreviousToken?: boolean;
    };
    ClearQueue: {
        behavior: AudioPlayerClearQueueBehaviour;
    };
    Init: undefined;
    PlayNext: undefined;
}

type AudioPlayerAction = ReducerAction<AudioPlayerActionType, AudioPlayerPayload>;

interface ShouldEnqueueParams {
    currentTrack: TestAudioTrack;
    nextTrack: TestAudioTrack;
    checkPreviousToken?: boolean;
}

const shouldEnqueue = ({ currentTrack, nextTrack, checkPreviousToken }: ShouldEnqueueParams) => {
    return !checkPreviousToken || currentTrack.token === nextTrack.expectedPreviousToken;
};

export const audioPlayerReducer: React.Reducer<AudioPlayerState, AudioPlayerAction> = (state, action) => {
    switch (action.type) {
        case 'Play': {
            const { track, behavior, checkPreviousToken } = action.payload;

            switch (behavior) {
                case AudioPlayerPlayBehaviour.REPLACE_ALL: {
                    return {
                        ...state,
                        currentTrack: track,
                        queue: [],
                    };
                }

                case AudioPlayerPlayBehaviour.ENQUEUE: {
                    const { currentTrack } = state;

                    if (!currentTrack) {
                        return {
                            ...state,
                            currentTrack: track,
                            queue: [],
                        };
                    }

                    if (
                        shouldEnqueue({
                            currentTrack,
                            checkPreviousToken,
                            nextTrack: track,
                        })
                    ) {
                        return {
                            ...state,
                            queue: [track],
                        };
                    }

                    return state;
                }
            }
        }

        case 'PlayNext': {
            return {
                ...state,
                currentTrack: state.queue[0] ?? state.currentTrack,
                queue: [],
            };
        }

        case 'ClearQueue': {
            const { behavior } = action.payload;

            switch (behavior) {
                case AudioPlayerClearQueueBehaviour.CLEAR_ALL: {
                    return {
                        ...state,
                        currentTrack: null,
                        queue: [],
                    };
                }

                case AudioPlayerClearQueueBehaviour.CLEAR_ENQUEUED: {
                    return {
                        ...state,
                        queue: [],
                    };
                }
            }
        }

        case 'Init': {
            return initialAudioPlayerState;
        }
    }
};
