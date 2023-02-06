import {makeCase, makeSuite} from 'ginny';
import {map} from 'ambar';
import {getReportQueries} from '@self/platform/spec/hermione/helpers/getBackendRequestParams';

export default makeSuite('Похожие товары', {
    story: {
        'Параметр "text".': {
            'По умолчанию': {
                'передаётся в репорт': makeCase({
                    params: {
                        place: 'Плейс репорта, в который уходят запросы',
                    },
                    async test() {
                        const {place} = this.params;
                        const queries = await getReportQueries(this, place);

                        await Promise.all(map(
                            query => this.expect(query).to.have.own.property(
                                'text',
                                'красный',
                                'Параметр "text" не передаётся в репорт'
                            ),
                            queries
                        ));
                    },
                }),
            },
        },
    },
});
