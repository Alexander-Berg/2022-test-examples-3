import { IUsersplit } from '../experiments/usersplit';
import { getVASUAASConfig } from './getVASUAASConfig';
import {
    mockVasSuperbundle,
    MOCKED_VAS_BUNDLE_PATH,
    MOCKED_STABLE_VERSION,
} from '../../../../tests/mockVasSuperbundle/mockVasSuperbundle';

mockVasSuperbundle();

describe('getVASUAASConfig', () => {
    it('should return default value if no data', () => {
        const usersplit: IUsersplit = {
            type: 'usersplit',
            headers: {},
        };
        const headers = {};

        expect(getVASUAASConfig(usersplit, headers, MOCKED_VAS_BUNDLE_PATH)).toEqual({
            expFlags: [],
            iCookie: undefined,
            testIds: undefined,
            stableVersion: MOCKED_STABLE_VERSION,
        });
    });

    it('should return correct value', () => {
        const usersplit: IUsersplit = {
            type: 'usersplit',
            headers: {
                'x-yandex-expflags': 'WwogICAgewogICAgICAiSEFORExFUiI6ICJWSURFT0FEU1NESyIsCiAgICAgICJDT05URVhUIjogewogICAgICAgICJGTEFHUyI6IHsKICAgICAgICAgICJFTkFCTEVfQ09ERUNTX1dISVRFTElTVCI6ICJUUlVFIgogICAgICAgIH0KICAgICAgfSwKICAgICAgIlRFU1RJRCI6IFsKICAgICAgICAiNDI5MDU5IgogICAgICBdCiAgICB9CiAgXQ==',
                'x-yandex-expboxes': '429059,0,-1',
            },
        };
        const headers = {
            'x-yandex-icookie-encrypted': 'iCookie',
        };
        const expectedResult = {
            expFlags: [
                [
                    {
                        HANDLER: 'VIDEOADSSDK',
                        CONTEXT: {
                            FLAGS: {
                                ENABLE_CODECS_WHITELIST: 'TRUE',
                            },
                        },
                        TESTID: ['429059'],
                    },
                ],
            ],
            iCookie: 'iCookie',
            testIds: '429059,0,-1',
            stableVersion: MOCKED_STABLE_VERSION,
        };
        const result = getVASUAASConfig(usersplit, headers, MOCKED_VAS_BUNDLE_PATH);

        expect(result).toEqual(expectedResult);
    });
});
