import {map} from 'ambar';
import {makeCase, makeSuite} from 'ginny';
import {getReportQueries} from '@self/platform/spec/hermione/helpers/getBackendRequestParams';

export default makeSuite('Карта', {
    story: {
        'Параметр "text".': {
            'По умолчанию': {
                'не передаётся в репорт': makeCase({
                    params: {
                        place: 'Плейс репорта, в который уходят запросы',
                        queryParams: 'Query параметры для идентификации запроса в логах',
                    },
                    async test() {
                        const {place, queryParams} = this.params;
                        const queries = await getReportQueries(
                            this,
                            place,
                            queryParams
                        );

                        await Promise.all(map(
                            query => this.expect(query).to.not.have.own.property(
                                'text',
                                'Параметр "text" передаётся в репорт'
                            ),
                            queries
                        ));
                    },
                }),
            },
        },
    },
});
