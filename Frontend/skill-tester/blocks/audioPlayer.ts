/* eslint-disable */
import { BlockType, Block } from './block';

export enum AudioPlayerAction {
    Play = 'Play',
    Stop = 'Stop',
    ClearQueue = 'ClearQueue',
}

interface AudioPlayerBlockPlayMeta {
    title: string;
    sub_title: string;
    art: {
        url: string;
    };
    background_image: {
        url: string;
    };
}

enum AudioPlayerBlockPlayBehaviour {
    REPLACE_ALL = 'REPLACE_ALL',
}

interface AudioPlayerBlockPlayData {
    action: AudioPlayerAction.Play;
    behavior: AudioPlayerBlockPlayBehaviour;
    item: {
        stream: {
            url: string;
            offset_ms: number;
            token: string;
            expected_previous_token?: string;
        };
        metadata?: Partial<AudioPlayerBlockPlayMeta>;
    };
}

interface AudioPlayerBlockStopData {
    action: AudioPlayerAction.Stop;
}

enum AudioPlayerBlockClearQueueBehaviour {
    CLEAR_ENQUEUED = 'CLEAR_ENQUEUED',
}

interface AudioPlayerBlockClearQueueData {
    action: AudioPlayerAction.ClearQueue;
    behavior: AudioPlayerBlockClearQueueBehaviour;
}

interface AudioPlayerActionDataMap {
    [AudioPlayerAction.Play]: AudioPlayerBlockPlayData;
    [AudioPlayerAction.Stop]: AudioPlayerBlockStopData;
    [AudioPlayerAction.ClearQueue]: AudioPlayerBlockClearQueueData;
}

export interface AudioPlayerBlock<A extends AudioPlayerAction = AudioPlayerAction> extends Block {
    type: BlockType.AudioPlayer;
    data: AudioPlayerActionDataMap[A];
}

export const isAudioPlayerBlock = <A extends AudioPlayerAction = AudioPlayerAction>(
    block: Block,
): block is AudioPlayerBlock<A> => {
    return block.type === BlockType.AudioPlayer;
};

export const extractAudioPlayerCommand = (blocks: Block[]) => {
    const block = blocks.find(isAudioPlayerBlock);

    return block;
};
