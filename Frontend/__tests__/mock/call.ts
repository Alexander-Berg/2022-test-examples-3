import { CallState } from '../../call';

export function callMockFactory() {
    return {
        createState: (state: Partial<CallState> = {}): CallState => ({
            isAvailable: false,
            duration: 0,
            minimized: false,
            videoEnabled: true,
            audioEnabled: true,
            displayAvailable: false,
            videoAvailable: true,
            audioAvailable: true,
            displayEnabled: false,
            hasLocalStream: false,
            hasRemoteStream: false,
            videoReasons: [],
            audioReasons: [],
            ...state,
        }),
    };
}
