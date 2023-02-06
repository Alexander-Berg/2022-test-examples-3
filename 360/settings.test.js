'use strict';

const settings = require('./settings.js');

const settingsMock = require('../../../test/mock/settings.json');
const settingsWithExternalEmailsMock = require('../../../test/mock/settings-with-emails.json');
const settingsGrassMock = require('../../../test/mock/settings-grass.json');
const yamailStatusMock = require('../../../test/mock/yamail-status.json');
const aiMock = require('../../../test/mock/ai.json');

const _cloneDeep = require('lodash/cloneDeep');

const status = require('../_helpers/status');

let core;
let mockMeta;
let mockSettings;

beforeEach(() => {
    mockMeta = jest.fn();
    mockSettings = jest.fn();
    core = {
        params: {},
        config: {
            sids: {
                corp: 'FAKE_CORP_SID',
                telemost: 'FAKE_TELEMOST_SID',
                promoteMail360: 'FAKE_PROMOTE_MAIL360_SID'
            }
        },
        service: (service) => {
            if (service === 'meta') {
                return mockMeta;
            }
            if (service === 'settings') {
                return mockSettings;
            }
        },
        status: status(core),
        auth: {
            get: jest.fn()
        }
    };
});

describe('-> TMP_FAIL если не ответил один из сервисов', () => {
    it('settings', async () => {
        mockSettings.mockRejectedValueOnce('');

        const res = await settings(core);

        expect(res.status.status).toBe(2);
    });

    it('meta', async () => {
        mockMeta.mockRejectedValueOnce('');

        const res = await settings(core);

        expect(res.status.status).toBe(2);
    });
});

test('-> OK', async () => {
    mockSettings.mockResolvedValueOnce(settingsMock);
    mockMeta.mockResolvedValueOnce(yamailStatusMock);
    core.auth.get.mockReturnValue(aiMock);

    const res = await settings(core);

    expect(res).toMatchSnapshot();
});

describe('reply_to (MMAPI-48)', () => {
    beforeEach(() => {
        core.auth.get.mockReturnValue(aiMock);
    });

    it('-> OK without emails', async () => {
        mockSettings.mockResolvedValueOnce(settingsMock);
        mockMeta.mockResolvedValueOnce(yamailStatusMock);

        const res = await settings(core);

        expect(res.settings_setup.body.reply_to).toMatchSnapshot();
    });

    it('-> OK with emails', async () => {
        mockSettings.mockResolvedValueOnce(settingsWithExternalEmailsMock);
        mockMeta.mockResolvedValueOnce(yamailStatusMock);

        const res = await settings(core);

        expect(res.settings_setup.body.reply_to).toMatchSnapshot();
    });
});

describe('с походом за подпиской в паспорт', () => {
    beforeEach(() => {
        core.auth.get.mockReturnValue({ ...aiMock, sids: [ '2' ] });
    });

    it('пасспорт ответил', async () => {
        mockSettings.mockResolvedValueOnce(settingsMock);
        mockMeta.mockResolvedValueOnce(yamailStatusMock);

        const res = await settings(core);

        expect(res).toMatchSnapshot();
    });

    it('пасспорт зафейлился', async () => {
        mockSettings.mockResolvedValueOnce(settingsMock);
        mockMeta.mockResolvedValueOnce(yamailStatusMock);

        const res = await settings(core);

        expect(res).toMatchSnapshot();
    });
});

// MOBILEMAIL-9461
test('переименует тему u2709 в grass', async () => {
    core.auth.get.mockReturnValue(aiMock);
    mockSettings.mockResolvedValueOnce(settingsGrassMock);

    const res = await settings(core);

    expect(res.settings_setup.body.color_scheme).toBe('grass');
});

// QUINN-5404
describe('если сервис настроек ответил пустым from_name', () => {
    beforeEach(() => {
        const mock = _cloneDeep(settingsMock);
        mock.settings.profile.single_settings.from_name = '';
        mockSettings.mockResolvedValueOnce(mock);
        core.auth.get.mockReturnValue(aiMock);
    });

    it('берет firstname + lastname из ai', async () => {
        const res = await settings(core);

        expect(res.settings_setup.body.from_name).toBe('Test Vs1');
    });
});

/* eslint-disable camelcase */
describe('telemost subscription', () => {
    beforeEach(() => {
        const mock = _cloneDeep(settingsMock);
        mockSettings.mockResolvedValueOnce(mock);
    });

    it('for telemost sid', async () => {
        core.auth.get.mockReturnValue({
            sids: [ core.config.sids.telemost ]
        });

        const { settings_setup: { body: { telemost_subscription } } } = await settings(core);

        expect(telemost_subscription).toBe(true);
    });

    it('for 669 sid', async () => {
        core.auth.get.mockReturnValue({
            sids: [ core.config.sids.corp ]
        });

        const { settings_setup: { body: { telemost_subscription } } } = await settings(core);

        expect(telemost_subscription).toBe(true);
    });

    it('checks config sids', async () => {
        core.config.sids = {};
        core.auth.get.mockReturnValue({
            sids: [ core.config.sids.corp ]
        });

        const { settings_setup: { body: { telemost_subscription } } } = await settings(core);

        expect(telemost_subscription).toBe(false);
    });
});

describe('promote_mail360', () => {
    beforeEach(() => {
        const mock = _cloneDeep(settingsMock);
        mockSettings.mockResolvedValueOnce(mock);
    });

    it('with promote_mail360 sid', async () => {
        core.auth.get.mockReturnValue({
            sids: [ core.config.sids.promoteMail360 ]
        });

        const { settings_setup: { body: { promote_mail360 } } } = await settings(core);

        expect(promote_mail360).toBe(true);
    });

    it('without promote_mail360 sid', async () => {
        core.auth.get.mockReturnValue({
            sids: []
        });

        const { settings_setup: { body: { promote_mail360 } } } = await settings(core);

        expect(promote_mail360).toBe(false);
    });

    it('without promote_mail360 sid in config', async () => {
        delete core.config.sids.promoteMail360;
        core.auth.get.mockReturnValue({
            sids: []
        });

        const { settings_setup: { body: { promote_mail360 } } } = await settings(core);

        expect(promote_mail360).toBe(false);
    });
});
