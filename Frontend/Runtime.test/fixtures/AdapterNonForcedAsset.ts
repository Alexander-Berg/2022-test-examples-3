import { AdapterTestRender as BaseTestRender } from './AdapterTestRender';

/**
 * Адаптер для проверки форсирования статики
 */
export function adapterTestRender(Base: typeof BaseTestRender) {
    return class AdapterTestRender extends Base {
        render() {
            return 'non-forced-assets-render2';
        }
    };
}
