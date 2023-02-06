import * as TapJsApi from '@yandex-int/tap-js-api';
import { editTags, getTags } from './apiSup';

jest.mock('./rum');

const fetchSpy = jest.spyOn(window, 'fetch');
const getUUID = jest.spyOn(TapJsApi, 'getUUID');
const getDeviceId = jest.spyOn(TapJsApi, 'getDeviceId');

const fetchResult = ({ json }: { json: object }) => ({
    json: () => Promise.resolve(json),
    ok: true
} as Response);

describe('lib', () => {
    describe('apiSup', () => {
        beforeEach(() => {
            fetchSpy.mockClear();
        });

        it('should throw errow without auth', async() => {
            getUUID.mockRejectedValueOnce(new Error('test error'));

            expect.assertions(2);
            await expect(getTags()).rejects.toThrowError('No uuid/deviceId obtained');
            expect(fetchSpy).toBeCalledTimes(0);
        });

        it('should list tags', async() => {
            const uuid = 'uuid_test';
            const did = 'did_test';
            const name = 'theme';
            const list = {
                name,
                value: [
                    'foo_off', 'weather_everyday_morning_on', 'weather_trend_on',
                    'assist_weather_push_on', 'assist_weather_auto_push_on', 'assist_weather_weekends_push_on',
                    'assist_weather_health_push_on', 'assist_weathertomorrow_push_off', 'assist_weather_nowcast_push_off'
                ],
                installId: uuid
            };

            getUUID.mockResolvedValueOnce(uuid);
            getDeviceId.mockResolvedValueOnce(did);
            fetchSpy.mockResolvedValueOnce(fetchResult({ json: list }));

            expect.assertions(3);
            await expect(getTags({ name })).resolves.toEqual(list);
            expect(fetchSpy).toBeCalledTimes(1);
            expect(fetchSpy).toHaveBeenLastCalledWith(
                expect.stringMatching(`tags/${did}/${uuid}/${name}`),
                {
                    body: null,
                    cache: 'reload',
                    credentials: 'omit',
                    headers: { Authorization: `Bearer uuid:${uuid}` },
                    method: 'GET',
                    signal: undefined
                }
            );
        });

        it('should edit tags', async() => {
            const uuid = 'uuid_test';
            const did = 'did_test';
            const name = 'theme';
            const topic = 'weather_everyday_morning';
            const list = [{
                name,
                value: [`${topic}_on`],
                installId: uuid
            }, {
                name,
                value: [`${topic}_off`],
                installId: uuid
            }];

            getUUID.mockResolvedValueOnce(uuid);
            getDeviceId.mockResolvedValueOnce(did);
            fetchSpy.mockResolvedValueOnce(fetchResult({ json: list }));

            expect.assertions(3);
            await expect(editTags({
                name,
                topics: [
                    { name: topic, action: 'off' }
                ]
            })).resolves.toEqual(list);
            expect(fetchSpy).toBeCalledTimes(1);
            expect(fetchSpy).toHaveBeenLastCalledWith(
                expect.stringMatching(`tags/${did}/${uuid}`),
                {
                    body: JSON.stringify([
                        { name, value: `${topic}_on`, op: 'remove' },
                        { name, value: `${topic}_off`, op: 'add' }
                    ]),
                    cache: 'reload',
                    credentials: 'omit',
                    headers: {
                        Authorization: `Bearer uuid:${uuid}`,
                        'Content-Type': 'application/json'
                    },
                    method: 'POST',
                    signal: undefined
                }
            );
        });
    });
});
