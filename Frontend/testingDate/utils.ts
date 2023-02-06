import * as moment from 'moment';
import { SerializedDevicesTestingRecord } from '../../../../serializers/devicesTestingRecord';
import { getAJVValidator } from '../../../../utils';
import { Range } from '../../../../db/tables/devicesTestingRecord';
import config from '../../../../services/config';
import { escapeUserInput, createUnorderedListForStartrek, escapeHighlightedInput } from '../../../../utils/startrek';
import { getRoleMembers } from '../../../../services/abc';

export const bodyValidator = getAJVValidator({
    oneOf: [
        {
            type: 'object',
            properties: {
                type: {
                    const: 'online',
                },
                options: {
                    type: 'object',
                    properties: {
                        selectedDateRange: {
                            type: 'object',
                            properties: {
                                fromMs: { type: 'number' },
                                toMs: { type: 'number' },
                            },
                            required: ['fromMs', 'toMs'],
                        },
                        login: { type: 'string' },
                        password: { type: 'string' },
                        skypeLogin: { type: 'string' },
                        comment: { type: 'string' },
                    },
                    required: ['selectedDateRange', 'login', 'password', 'skypeLogin'],
                },
            },
            required: ['type', 'options'],
        },
        {
            type: 'object',
            properties: {
                type: {
                    const: 'offline',
                },
            },
            required: ['type'],
        },
    ],
});

export const formatDevicesTestingRange = ({ fromMs, toMs }: Range, withDate: boolean = false) => {
    const utcOffset = config.startrek.smartHomeTestDateUtcOffest;

    const timeStr = `с\xa0${moment(fromMs)
        .utcOffset(utcOffset)
        .format('HH:mm')} до\xa0${moment(toMs)
        .utcOffset(utcOffset)
        .format('HH:mm')}`;

    return withDate ?
        `${moment(fromMs)
            .utcOffset(utcOffset)
            .format('DD.MM.YYYY')}\xa0${timeStr}` :
        timeStr;
};

export const getTestingDateComment = (record: SerializedDevicesTestingRecord) => {
    const intro = `Пользователь выбрал %%${record.type}%% тестирование.`;

    if (record.type === 'offline') {
        return intro;
    }

    const { login, password, comment, selectedDateRange, skypeLogin, lang } = record.options;

    const params = `====== Данные:\n${createUnorderedListForStartrek([
        `Дата: %%${formatDevicesTestingRange(selectedDateRange, true)}%%`,
        `Логин: %%${escapeHighlightedInput(login)}%%`,
        `Пароль: %%${escapeHighlightedInput(password)}%%`,
        `Логин в скайпе: %%${escapeHighlightedInput(skypeLogin)}%%`,
        `Комментарий: ${comment ? escapeUserInput(comment) : '–'}`,
        `Язык: ${lang}`,
    ])}`;

    return `${intro}\n${params}`;
};

export const getTestingDateCommentSummonees = async(record: SerializedDevicesTestingRecord) => {
    const roles = config.startrek.smartHomeTestDateSummonees.abc.roles;
    // eslint-disable-next-line no-nested-ternary
    const roleId = record.type === 'offline' ? roles.offline :
        record.options.lang === 'en' ? roles.en : roles.online;

    const { results } = await getRoleMembers(roleId);
    return results.map(({ person: { login } }) => login);
};
