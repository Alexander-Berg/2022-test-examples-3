import { TLD } from '@yandex-int/express-yandex-csp';
import { CSPDirectives } from 'csp-header';

const policies: Partial<CSPDirectives> = {
    'script-src': [`pass-test.yandex.${TLD}`, 'social.yandex.ru'],
    'connect-src': [`api.passport.yandex.${TLD}`],
};

export default policies;
