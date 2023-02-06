/* eslint-disable */
export enum BlockType {
    Suggest = 'suggest',
    DivCard = 'div_card',
    AudioPlayer = 'audio_player',
}

export interface Block {
    type: BlockType;
}
