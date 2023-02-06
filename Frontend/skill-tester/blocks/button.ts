/* eslint-disable */
import { Block, BlockType } from './block';

interface ButtonBlockData {
    hide: boolean;
    payload?: any;
    title: string;
    url?: string;
    isAccountLinkingRequest?: boolean;
}

export interface ButtonBlock extends Block {
    data: ButtonBlockData;
    suggest_type: 'external_skill' | 'skill_account_linking_button';
    type: BlockType.Suggest;
}
