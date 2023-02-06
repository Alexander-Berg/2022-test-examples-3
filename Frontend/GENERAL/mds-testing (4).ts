import { CSPDirectives } from 'csp-header';

const policies: Partial<CSPDirectives> = {
    'img-src': ['avatars.mdst.yandex.net'],
};

export default policies;
