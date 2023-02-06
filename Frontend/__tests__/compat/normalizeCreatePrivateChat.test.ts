import { normalizeCreatePrivateChat } from '../../compat';
import { CREATE_PRIVATE_CHAT_NORMAL_RESPONSE, CREATE_PRIVATE_CHAT_BAD_RESPONSE } from './normalizeCreatePrivateChat.data';

describe('#normalizeCreatePrivateChat', () => {
    it('Нормализация полного ответа', () => {
        const { chat } = normalizeCreatePrivateChat(CREATE_PRIVATE_CHAT_NORMAL_RESPONSE);
        const users = chat.chat_id.split('_');

        expect(chat.member_count).toBe(2);

        expect(chat.members).toEqual(expect.arrayContaining(users));
    });

    it('Нормализация неполного ответа', () => {
        const { chat } = normalizeCreatePrivateChat(CREATE_PRIVATE_CHAT_BAD_RESPONSE);
        const users = chat.chat_id.split('_');

        expect(chat.member_count).toBe(2);

        expect(chat.members).toEqual(expect.arrayContaining(users));
    });
});
