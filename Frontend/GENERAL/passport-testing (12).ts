import { TLD } from '@yandex-int/express-yandex-csp';
import { CSPDirectives } from 'csp-header';

const policies: CSPDirectives = {
    'script-src': [`pass-test.yandex.${TLD}`, 'social.yandex.ru'],
};

export default policies;
