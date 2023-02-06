'use strict';

const staffRequest = require('./staff-request');

let core;
let staffApiService;

let isYandexTeamEmailDomain;
const nowISOString = '2020-03-30T00:00:00';

const staffApiServiceResponseMock = {
    department_group: {
        department: {
            name: {
                full: {
                    ru: 'TEST_DEPARTMENT_GROUP',
                    en: 'TEST_DEPARTMENT_GROUP_EN'
                }
            }
        }
    },
    official: { position: { en: 'TEST_POSITION_EN', ru: 'TEST_POSITION_RU' } },
    phones: [ { number: 'FAKE_PHONE_NUMBER' } ],
    work_phone: 21973
};
const gapsModelResponseMock = [ {
    comment: '',
    workflow: 'absence',
    work_in_absence: true,
    date_from: '2020-03-30T00:00:00',
    date_to: '2020-04-04T00:00:00',
    full_day: true,
    color: '#ffc136'
} ];
let staffIsExternalModelResponseMock = false;

beforeEach(() => {
    staffApiService = jest.fn();

    isYandexTeamEmailDomain = jest.fn();
    isYandexTeamEmailDomain.mockReturnValue(true);

    core = {
        config: {
            IS_CORP: true,
            domainConfig: {
                isYandexTeamEmailDomain
            },
            locale: 'ru'
        },
        auth: {
            get: () => ({ login: 'TEST_LOGIN' })
        },
        service: () => staffApiService,
        request: jest.fn(),

        console: {
            error: jest.fn()
        }
    };

    jest.spyOn(Date.prototype, 'toISOString').mockImplementation(() => nowISOString);
});

afterAll(() => {
    Date.prototype.toISOString.mockRestore();
});

test('ходит в staff-api за данными пользователя', async () => {
    staffApiService.mockResolvedValueOnce(staffApiServiceResponseMock);
    core.request.mockResolvedValueOnce(staffIsExternalModelResponseMock);
    core.request.mockResolvedValueOnce(gapsModelResponseMock);

    await staffRequest('diadorer@yandex-team.ru', core);

    expect(staffApiService.mock.calls).toMatchSnapshot();
});

test('запрашивает модели staff-is-external и gap', async () => {
    staffApiService.mockResolvedValueOnce(staffApiServiceResponseMock);
    core.request.mockResolvedValueOnce(staffIsExternalModelResponseMock);
    core.request.mockResolvedValueOnce(gapsModelResponseMock);

    await staffRequest('diadorer@yandex-team.ru', core);

    expect(core.request.mock.calls).toMatchSnapshot();
});

describe('для не yandex-team домена ходит за логином в staff-api и', () => {
    beforeEach(() => {
        isYandexTeamEmailDomain.mockReturnValue(false);
    });

    it('забирает его, в случае успеха', async () => {
        staffApiService.mockResolvedValueOnce({ login: 'FAKE_LOGIN' });
        staffApiService.mockResolvedValueOnce(staffApiServiceResponseMock);
        core.request.mockResolvedValueOnce(staffIsExternalModelResponseMock);
        core.request.mockResolvedValueOnce(gapsModelResponseMock);

        const res = await staffRequest('diadorer@yamoney.ru', core);

        expect(staffApiService.mock.calls).toMatchSnapshot();
        expect(res.login).toBe('FAKE_LOGIN');
    });

    it('фолбечит в случае ошибки', async () => {
        isYandexTeamEmailDomain.mockReturnValue(false);

        staffApiService.mockRejectedValueOnce(new Error('Some Error'));
        staffApiService.mockResolvedValueOnce(staffApiServiceResponseMock);
        core.request.mockResolvedValueOnce(staffIsExternalModelResponseMock);
        core.request.mockResolvedValueOnce(gapsModelResponseMock);

        const res = await staffRequest('diadorer@yamoney.ru', core);

        expect(res.login).toBe('diadorer');
    });

    it('фолбечит, если вернулся пустой ответ', async () => {
        isYandexTeamEmailDomain.mockReturnValue(false);

        staffApiService.mockResolvedValueOnce({ login: '' });
        staffApiService.mockResolvedValueOnce(staffApiServiceResponseMock);
        core.request.mockResolvedValueOnce(staffIsExternalModelResponseMock);
        core.request.mockResolvedValueOnce(gapsModelResponseMock);

        const res = await staffRequest('diadorer@yamoney.ru', core);

        expect(res.login).toBe('diadorer');
    });
});

test('работает', async () => {
    staffApiService.mockResolvedValueOnce(staffApiServiceResponseMock);
    core.request.mockResolvedValueOnce(staffIsExternalModelResponseMock);
    core.request.mockResolvedValueOnce(gapsModelResponseMock);

    const res = await staffRequest('diadorer@yadnex-team.ru', core);

    expect(res).toMatchSnapshot();
});

test('прячет персональные данные от внешних сотрудников', async () => {
    staffIsExternalModelResponseMock = true;

    staffApiService.mockResolvedValueOnce(staffApiServiceResponseMock);
    core.request.mockResolvedValueOnce(staffIsExternalModelResponseMock);
    core.request.mockResolvedValueOnce(gapsModelResponseMock);

    const { phones } = await staffRequest('diadorer@yadnex-team.ru', core);

    expect(phones).toEqual(expect.objectContaining({
        mobile: []
    }));
});

test('возвращает null в случае проблем с staff-api', async () => {
    staffApiService.mockRejectedValueOnce(new Error('Error occured'));
    core.request.mockResolvedValueOnce(staffIsExternalModelResponseMock);
    core.request.mockResolvedValueOnce(gapsModelResponseMock);

    const res = await staffRequest('diadorer@yadnex-team.ru', core);

    expect(core.console.error).toBeCalledTimes(1);
    expect(res).toBeNull();
});
