import { CSPDirectives } from 'csp-header';
import { TLD } from '@yandex-int/express-yandex-csp';

const policies: CSPDirectives = {
    'script-src': [`pass-test.yandex.${TLD}`, 'social.yandex.ru'],
};

export default policies;
