import { IEventInfo } from './calendarApi/types';
import { generateChannelName, CHANNEL_NAME_MAX_LEN } from './generateChannelName';

describe('generateChannelName', () => {
    it('должен правильно генерировать имя канала', () => {
        const eventInfo: IEventInfo = {
            name: 'Тестовое Имя События',
            id: 123456,
            externalId: 'asd13',
            othersCanView: true,
            type: 'user',
            startTs: '',
            endTs: '',
            attendees: [],
            notifications: [],
            decision: 'yes',
        };

        expect(generateChannelName(eventInfo)).toEqual('тестовое_имя_события-123456');
    });

    it('должен обрезать длинные имена каналов', () => {
        const eventInfo: IEventInfo = {
            name: 'Тестовое Имя События  очень очень очень очень длинное имя ну просто супер какое длинное',
            id: 123456,
            externalId: 'asd13',
            othersCanView: true,
            type: 'user',
            startTs: '',
            endTs: '',
            attendees: [],
            notifications: [],
            decision: 'yes',
        };

        const generatedChannelName = generateChannelName(eventInfo);

        expect(generatedChannelName.length).toEqual(CHANNEL_NAME_MAX_LEN);
        expect(generatedChannelName).toEqual('тестовое_имя_события__очень_очень_очень_очень_длинное_имя_ну_просто_супер-123456');
    });
});
