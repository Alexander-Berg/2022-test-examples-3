/* eslint-disable max-len */

const {
    generateSearchTextsFromUrl,
    getUrlWithoutExcessQueryParams,
} = require('../../../../../../src/helper/generate-search-texts-from-url');

describe('generate search texts for search by url:', () => {
    const testData = [
        {
            actual: 'http://teledvor.ru/index.php?route=product/product&product_id=1374&lalala=lalala',
            expected: [],
        },
        {
            actual: 'https://www.ozon-example.ru/context/detail/id/144064187/',
            expected: [],
        },
        {
            actual: 'https://www.ozon.ru/context/detail/id/144064187/',
            expected: ['url:"ozon.ru/context/detail/id/144064187*"', 'url:"www.ozon.ru/context/detail/id/144064187*"'],
        },
        {
            actual: 'http://pcshop.ru/view.php?id=32521',
            expected: [],
        },
        {
            actual: 'http://sidex.ru/view.php?id=32521',
            expected: ['url:"sidex.ru/view.php?id=32521*"', 'url:"www.sidex.ru/view.php?id=32521*"'],
        },
        {
            actual: 'http://ht-comp.ru/?code=123',
            expected: ['url:"ht-comp.ru/?code=123*"', 'url:"www.ht-comp.ru/?code=123*"'],
        },
        {
            actual: 'http://teledvor.ru/index.php?route=product/product',
            expected: [],
        },
        {
            actual: 'http://www.teledvor.ru/index.php?route=product/product&product_id=1374&lalala=lalala',
            expected: [],
        },
        {
            actual: 'http://m.teledvor.ru/index.php?route=product/product&product_id=1374&lalala=lalala',
            expected: [],
        },
        {
            actual: 'https://www.m.ozon-example.ru/context/detail/id/144064187/',
            expected: [],
        },
        {
            actual: 'https://www.ozon.ru/context/detail/id/144064187/',
            expected: ['url:"ozon.ru/context/detail/id/144064187*"', 'url:"www.ozon.ru/context/detail/id/144064187*"'],
        },
        {
            actual: 'http://pcshop.ru/view.php?id=32521',
            expected: [],
        },
        {
            actual: 'http://www.m.sidex.ru/view.php?id=32521',
            expected: ['url:"sidex.ru/view.php?id=32521*"', 'url:"www.sidex.ru/view.php?id=32521*"'],
        },
        {
            actual: 'http://sidex.ru/view.php?id=32521',
            expected: ['url:"sidex.ru/view.php?id=32521*"', 'url:"www.sidex.ru/view.php?id=32521*"'],
        },
        {
            actual: 'http://m.sidex.ru/view.php?id=32521',
            expected: ['url:"sidex.ru/view.php?id=32521*"', 'url:"www.sidex.ru/view.php?id=32521*"'],
        },
        {
            actual: 'http://m.ht-comp.ru/?code=123',
            expected: ['url:"ht-comp.ru/?code=123*"', 'url:"www.ht-comp.ru/?code=123*"'],
        },
        {
            actual: 'http://ht-comp.ru/?code=123',
            expected: ['url:"ht-comp.ru/?code=123*"', 'url:"www.ht-comp.ru/?code=123*"'],
        },
        {
            actual: 'http://teledvor.ru/index.php?route=product/product',
            expected: [],
        },
        {
            actual: 'http://www.teledvor.ru/index.php?route=product/product&product_id=1374&lalala=lalala',
            expected: [],
        },
        {
            actual: 'http://www.teledvor.ru/index.php?route=product/product&product_id=1374&lalala=lalala',
            expected: [],
        },
    ];

    testData.forEach(({ actual, expected }) => {
        it(`'${actual}' => '${expected}'`, () => {
            expect(generateSearchTextsFromUrl(actual)).toEqual(expected);
        });
    });
});

describe('remove excess query params from url:', () => {
    const testData = [
        {
            actual: 'http://teledvor.ru/index.php?route=product/product&product_id=1374&lalala=lalala',
            expected: 'teledvor.ru/index.php?route=product%2Fproduct&product_id=1374',
        },
        {
            actual: 'https://www.ozon-example.ru/context/detail/id/144064187/',
            expected: 'ozon-example.ru/context/detail/id/144064187',
        },
        {
            actual: 'https://www.ozon.ru/context/detail/id/144064187/',
            expected: 'ozon.ru/context/detail/id/144064187',
        },
        {
            actual: 'https://www.ozon.ru/context/detail/id/144064187?utm_lala=blah&utm_medium=meh',
            expected: 'ozon.ru/context/detail/id/144064187',
        },
        {
            actual: 'http://pcshop.ru/view.php?id=32521',
            expected: 'pcshop.ru/view.php?id=32521',
        },
        {
            actual: 'http://sidex.ru/view.php?id=32521',
            expected: 'sidex.ru/view.php?id=32521',
        },
        {
            actual: 'http://ht-comp.ru/?code=123',
            expected: 'ht-comp.ru/?code=123',
        },
        {
            actual: 'http://www.teledvor.ru/index.php?route=product/product&product_id=1374&lalala=lalala',
            expected: 'teledvor.ru/index.php?route=product%2Fproduct&product_id=1374',
        },
        {
            actual: 'http://testdomain1.com/?param1=1&param2=2',
            expected: 'http://testdomain1.com/?param1=1&param2=2',
        },
        {
            actual: 'http://testdomain2.com/index.php?param1=1&param2=2',
            expected: 'http://testdomain2.com/index.php?param1=1&param2=2',
        },
        {
            actual: 'https://beru.ru/product/pylesos-philips-fc6728-speedpro-aqua-matovo-sinii-metallik/100656718814',
            expected: 'beru.ru/product/100656718814',
        },
        {
            actual: 'https://www.pleer.ru/product_503596_Xiaomi_Mi_Power_Bank_2_PLM09ZM_10000mAh_Silver.html',
            expected: 'pleer.ru/_503596_Xiaomi_Mi_Power_Bank_2_PLM09ZM_10000mAh_Silver.html',
        },
        {
            actual: 'http://teledvor.ru/index.php?route=product/product&product_id=1374&lalala=lalala',
            expected: 'teledvor.ru/index.php?route=product%2Fproduct&product_id=1374',
        },
        {
            actual: 'https://www.ozon-example.ru/context/detail/id/144064187/',
            expected: 'ozon-example.ru/context/detail/id/144064187',
        },
        {
            actual: 'https://www.ozon.ru/context/detail/id/144064187/',
            expected: 'ozon.ru/context/detail/id/144064187',
        },
        {
            actual: 'https://www.ozon.ru/context/detail/id/144064187?utm_lala=blah&utm_medium=meh',
            expected: 'ozon.ru/context/detail/id/144064187',
        },
        {
            actual: 'http://pcshop.ru/view.php?id=32521',
            expected: 'pcshop.ru/view.php?id=32521',
        },
        {
            actual: 'http://m.sidex.ru/view.php?id=32521',
            expected: 'sidex.ru/view.php?id=32521',
        },
        {
            actual: 'http://ht-comp.ru/?code=123',
            expected: 'ht-comp.ru/?code=123',
        },
        {
            actual: 'http://www.teledvor.ru/index.php?route=product/product&product_id=1374&lalala=lalala',
            expected: 'teledvor.ru/index.php?route=product%2Fproduct&product_id=1374',
        },
        {
            actual: 'http://testdomain1.com/?param1=1&param2=2',
            expected: 'http://testdomain1.com/?param1=1&param2=2',
        },
        {
            actual: 'http://testdomain2.com/index.php?param1=1&param2=2',
            expected: 'http://testdomain2.com/index.php?param1=1&param2=2',
        },
        {
            actual: 'https://m.beru.ru/product/pylesos-philips-fc6728-speedpro-aqua-matovo-sinii-metallik/100656718814',
            expected: 'beru.ru/product/100656718814',
        },
        {
            actual: 'https://www.m.pleer.ru/product_503596_Xiaomi_Mi_Power_Bank_2_PLM09ZM_10000mAh_Silver.html',
            expected: 'pleer.ru/_503596_Xiaomi_Mi_Power_Bank_2_PLM09ZM_10000mAh_Silver.html',
        },
    ];

    testData.forEach(({ actual, expected }) => {
        it(`'${actual}' => '${expected}'`, () => {
            expect(getUrlWithoutExcessQueryParams(actual)).toEqual(expected);
        });
    });
});
