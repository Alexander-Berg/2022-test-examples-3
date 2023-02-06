import { Adapter } from '../../..';

/**
 * Адаптер с "render" и непустым "transform"
 */
export class AdapterTestFull extends Adapter {
    render() {
        return 'full-render';
    }

    transform() {
        return { block: 'full-transform' };
    }
}
