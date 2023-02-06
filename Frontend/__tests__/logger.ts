/* eslint-disable no-console */
import { configureLogger } from '@yandex-int/messenger.logger-facade';

configureLogger((name) => ({
    log: (...args) => console.log(name, ...args),
    error: (...args) => console.log(name, ...args),
}));
