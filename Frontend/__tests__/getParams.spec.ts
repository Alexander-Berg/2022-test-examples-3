import { getParams } from '../getParams';

const location = {
    host: 'modadvert-test.yandex-team.ru',
    hostname: 'modadvert-test.yandex-team.ru',
    href: 'https://modadvert-test.yandex-team.ru/service/direct/search?autoEditSelect=false&pageNum=1&pageSize=50&q=%7B%22type%22%3A%5B%22banner%22%2C%22text_sm%22%5D%2C%22related_objects%22%3A%7B%22types%22%3A%5B%5D%7D%2C%22campaign_id%22%3A%5B42509534%5D%2C%22without_highlight%22%3Atrue%7D&viewMode=compact',
    pathname: '/service/direct/search',
    search: '?autoEditSelect=false&pageNum=1&pageSize=50&q=%7B%22type%22%3A%5B%22banner%22%2C%22text_sm%22%5D%2C%22related_objects%22%3A%7B%22types%22%3A%5B%5D%7D%2C%22campaign_id%22%3A%5B42509534%5D%2C%22without_highlight%22%3Atrue%7D&viewMode=compact',
    state: '',
    hash: '',
};

describe('routes/getParams', () => {
    it('match returns query and path', () => {
        location.pathname = '/service/direct/search';
        expect(getParams(location, 'service/search')).toEqual(
            {
                path: {
                    service: 'direct',
                },
                query: {
                    autoEditSelect: 'false',
                    pageNum: '1',
                    pageSize: '50',
                    q: '{"type":["banner","text_sm"],"related_objects":{"types":[]},"campaign_id":[42509534],"without_highlight":true}',
                    viewMode: 'compact',
                },
            },
        );
    });

    it('not match returns null', () => {
        location.pathname = '/service/direct/searchHAHA';
        expect(getParams(location, 'service/search')).toEqual(null);
    });
});
