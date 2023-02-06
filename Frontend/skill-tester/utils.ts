/* eslint-disable */
import { stringify } from 'querystring';
import config from '../config';
import { SettingsSchema } from '../../db/tables/settings';
import { DivCardBlock } from './blocks/card';
import { ButtonBlock } from './blocks/button';
import { BlockType } from './blocks/block';
import { AudioPlayerBlock, AudioPlayerAction } from './blocks/audioPlayer';

type ButtonOrCardBlock = DivCardBlock | ButtonBlock;

export function extractCardAndButtons(blocks: ButtonOrCardBlock[], info: SettingsSchema<any>) {
    const buttons: Array<ButtonBlock['data']> = [];
    let card: DivCardBlock | undefined;
    let isAccountLinkingRequest;

    for (const block of blocks) {
        if (block.type === BlockType.DivCard) {
            card = block;
        } else if (block.type === BlockType.Suggest && block.suggest_type === 'external_skill') {
            buttons.push(block.data);
        } else if (block.type === BlockType.Suggest && block.suggest_type === 'skill_account_linking_button') {
            if (!info.oauthApp || !info.oauthApp.socialAppName) {
                throw new Error('Invalid operation');
            }

            const url = `${config.skillAuth.bindUrl}?${stringify({
                consumer: 'passport',
                application_name: info.oauthApp.socialAppName,
            })}`;

            buttons.push({
                title: 'АВТОРИЗОВАТЬСЯ',
                url,
                hide: false,
                isAccountLinkingRequest: true,
            });

            isAccountLinkingRequest = true;
        }
    }

    return {
        buttons,
        card,
        isAccountLinkingRequest,
    };
}

export const processAudioPlayerBlockForResponse = (block: AudioPlayerBlock) => {
    switch (block.data.action) {
        case AudioPlayerAction.Play:
            return {
                action: block.data.action,
                behavior: block.data.behavior,
                item: block.data.item,
            };
        case AudioPlayerAction.Stop:
            return {
                action: block.data.action,
            };

        case AudioPlayerAction.ClearQueue:
            return {
                action: block.data.action,
                behavior: block.data.behavior,
            };
    }
};
