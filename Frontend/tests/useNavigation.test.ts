import { cleanupParams } from '../hooks/useNavigation';

describe('useNavigation', () => {
    describe('cleanupParams', () => {
        it('корректно очищает параметры', () => {
            expect(cleanupParams({
                disauto: ['glfilter'],
                glfilter: ['7893318:13907364'],
                hid: undefined,
                nid: undefined,
                order: 'aprice',
                pricefrom: undefined,
                priceto: undefined,
                query_source: 'main_page',
                rs: undefined,
                text: 'Наушники',
                used_goods: undefined,
            })).toStrictEqual({
                disauto: ['glfilter'],
                glfilter: ['7893318:13907364'],
                hid: undefined,
                nid: undefined,
                order: 'aprice',
                pricefrom: undefined,
                priceto: undefined,
                rs: undefined,
                text: 'Наушники',
                used_goods: undefined,
            });
        });
    });
});
