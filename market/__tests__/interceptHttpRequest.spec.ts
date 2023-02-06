/**
 * @jest-environment jsdom
 */

import {isUndefined} from '../typeGuards';
import {
    addInterceptDataItem,
    canInjectMock,
    clearInterceptData,
    disableInterception,
    enableInterception,
    getDevMockName,
    getInterceptData,
    getInterceptDataByConditions,
    HttpMethod,
    injectMocks,
    InterceptData,
    InterceptKeys,
    InterceptRequestTypes,
    isRegisteredMock,
    registerMock,
    removeDevMocks,
    removeInterceptDataItemByIdx,
    removeInterceptDataItemsByUrl,
    setCustomStorage,
    setDevMock,
    shouldInterceptHttpRequests,
} from '../interceptHttpRequest';

class SimpleStorage {
    private values = {};

    public length = 0;

    public getItem = key => {
        return JSON.parse(this.values[key] || undefined);
    };

    public setItem = (key: string, value: string) => {
        if (isUndefined(this.values[key])) this.length++;

        this.values[key] = JSON.stringify(value);
    };
}

const initStorage = () => {
    const storage = new SimpleStorage();

    setCustomStorage(storage);
};

const URL_1 = 'https://ow.tst.market.yandex-team.ru/api/order/xxxxxxxxx/communication';

const ITEM1: InterceptData = {
    timeout: 10000,
    method: HttpMethod.GET,
    url: '/communication',
    status: 504,
    type: InterceptRequestTypes.EMULATE_HTTP_REQUEST,
};
const ITEM2: InterceptData = {
    timeout: 20000,
    method: HttpMethod.POST,
    url: '/communication',
    status: 504,
    type: InterceptRequestTypes.EMULATE_HTTP_REQUEST,
};
const ITEM3: InterceptData = {
    timeout: 10000,
    method: HttpMethod.GET,
    url: '/contacts',
    status: 504,
    type: InterceptRequestTypes.EMULATE_HTTP_REQUEST,
};
const ITEM4: InterceptData = {
    timeout: 20000,
    method: HttpMethod.GET,
    url: '/communication',
    status: 504,
    type: InterceptRequestTypes.EMULATE_HTTP_REQUEST,
};

const ITEM5: InterceptData = {
    timeout: 200,
    method: HttpMethod.GET,
    url: '/communication',
    status: 504,
    type: InterceptRequestTypes.MOCK_BODY,
    body: '{"fld": "some JSON"}',
};
const ITEM6: InterceptData = {
    timeout: 200,
    method: HttpMethod.POST,
    url: '/communication',
    status: 504,
    type: InterceptRequestTypes.MOCK_BODY,
    params: '{"param1": "some value", "param2": "some other value"}',
    body: '{"fld": "some JSON"}',
};

const MOCK_NAME = 'aaa/bbb/ccc/__tests__';

describe('Interception', () => {
    it('Enabled', () => {
        enableInterception();
        expect(shouldInterceptHttpRequests()).toEqual(true);
    });
    it('Disabled', () => {
        disableInterception();
        expect(shouldInterceptHttpRequests()).toEqual(false);
    });
    it('Cleared', () => {
        clearInterceptData();
        expect(getInterceptData(InterceptKeys.DATA)).toEqual([]);
    });
    it('Adds item', () => {
        initStorage();
        addInterceptDataItem(ITEM1);
        expect(getInterceptData(InterceptKeys.DATA)).toContainEqual(ITEM1);
    });
    it('Removes item by idx', () => {
        initStorage();
        let c = 0;

        c += +addInterceptDataItem(ITEM1);
        c += +addInterceptDataItem(ITEM2);
        c += +addInterceptDataItem(ITEM3);
        expect(c).toEqual(3);
        expect(getInterceptData(InterceptKeys.DATA)).toHaveLength(3);

        removeInterceptDataItemByIdx(1);

        const data = getInterceptData(InterceptKeys.DATA);

        expect(data).toHaveLength(2);
        expect(data).toContainEqual(ITEM1);
        expect(data).toContainEqual(ITEM3);
        expect(data).not.toContainEqual(ITEM2);
    });
    it('Removes item by url', () => {
        initStorage();
        addInterceptDataItem(ITEM1);
        addInterceptDataItem(ITEM2);
        addInterceptDataItem(ITEM3);
        expect(getInterceptData(InterceptKeys.DATA)).toHaveLength(3);
        removeInterceptDataItemsByUrl(ITEM3.url);

        const data = getInterceptData(InterceptKeys.DATA);

        expect(data).toHaveLength(2);
        expect(data).toContainEqual(ITEM1);
        expect(data).toContainEqual(ITEM2);
        expect(data).not.toContainEqual(ITEM3);
    });
    it('Filters data by type, method & url', () => {
        initStorage();
        addInterceptDataItem(ITEM1);
        addInterceptDataItem(ITEM2);
        addInterceptDataItem(ITEM3);
        addInterceptDataItem(ITEM4);
        addInterceptDataItem(ITEM5);
        addInterceptDataItem(ITEM6);

        const data = getInterceptDataByConditions(
            InterceptRequestTypes.EMULATE_HTTP_REQUEST,
            URL_1,
            'GET' as HttpMethod
        );

        expect(data).toHaveLength(2);
        expect(data).toContainEqual(ITEM1);
        expect(data).toContainEqual(ITEM4);

        const data1 = getInterceptDataByConditions(
            InterceptRequestTypes.EMULATE_HTTP_REQUEST,
            URL_1,
            'POST' as HttpMethod
        );

        expect(data1).toHaveLength(1);
        expect(data1).toContainEqual(ITEM2);
    });

    it('Filters data by type, method, url & params', () => {
        initStorage();
        addInterceptDataItem(ITEM1);
        addInterceptDataItem(ITEM2);
        addInterceptDataItem(ITEM3);
        addInterceptDataItem(ITEM4);
        addInterceptDataItem(ITEM5);
        addInterceptDataItem(ITEM6);

        const data = getInterceptDataByConditions(InterceptRequestTypes.MOCK_BODY, URL_1, 'GET' as HttpMethod);

        expect(data).toHaveLength(1);
        expect(data).toContainEqual(ITEM5);

        const data1 = getInterceptDataByConditions(
            InterceptRequestTypes.MOCK_BODY,
            URL_1,
            'POST' as HttpMethod,
            ITEM6.params
        );

        expect(data1).toHaveLength(1);
        expect(data1).toContainEqual(ITEM6);
    });

    it('Checks all cases for getDevMockName()', () => {
        initStorage();

        const mockName = getDevMockName(MOCK_NAME);

        expect(mockName).toEqual('__tests__');
        expect(getDevMockName(mockName)).toEqual(mockName);
        expect(() => getDevMockName('')).toThrow();
    });

    it('Checks registerMock()', () => {
        initStorage();

        expect(isRegisteredMock(MOCK_NAME)).toEqual(false);
        registerMock(MOCK_NAME);
        expect(isRegisteredMock(MOCK_NAME)).toEqual(true);
    });

    it('Checks canInjectMock(), setDevMock()', () => {
        initStorage();

        const enableDevMock = setDevMock(true);
        const disableDevMock = setDevMock(false);

        registerMock(MOCK_NAME);

        expect(canInjectMock(MOCK_NAME)).toEqual(false);

        enableDevMock(MOCK_NAME);

        expect(canInjectMock(MOCK_NAME)).toEqual(true);

        disableDevMock(MOCK_NAME);

        expect(canInjectMock(MOCK_NAME)).toEqual(false);
        expect(() => enableDevMock(`${MOCK_NAME}xxx`)).toThrow();
    });

    it('Checks injectMocks(), removeDevMocks()', () => {
        initStorage();
        registerMock(MOCK_NAME);
        setDevMock(true)(MOCK_NAME);

        expect(canInjectMock(MOCK_NAME)).toEqual(true);

        injectMocks(MOCK_NAME, [ITEM5, ITEM6]);

        expect(getInterceptData(InterceptKeys.DATA)).toHaveLength(2);
        // canInjectMock удаляет прежние моки с заданным именем.
        expect(canInjectMock(MOCK_NAME)).toEqual(true);
        expect(getInterceptData(InterceptKeys.DATA)).toHaveLength(0);

        injectMocks(MOCK_NAME, [ITEM5, ITEM6]);

        expect(getInterceptData(InterceptKeys.DATA)).toHaveLength(2);

        removeDevMocks(MOCK_NAME);

        expect(getInterceptData(InterceptKeys.DATA)).toHaveLength(0);
    });
});
