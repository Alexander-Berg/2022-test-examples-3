import { CSPDirectives } from 'csp-header';

const policies: Partial<CSPDirectives> = {
    'img-src': ['contest.test.yandex.ru'],
};

export default policies;
