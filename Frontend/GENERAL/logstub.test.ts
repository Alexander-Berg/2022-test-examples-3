import { logging } from './logstub';

describe('logstub', () => {
    it('Should do nothing', () => {
        // eslint-disable-next-line no-console
        console.debug = jest.fn();
        // eslint-disable-next-line no-console
        console.info = jest.fn();
        // eslint-disable-next-line no-console
        console.warn = jest.fn();
        // eslint-disable-next-line no-console
        console.error = jest.fn();

        logging.debug(1);
        logging.info(1);
        logging.warn(1);
        logging.error(1);

        // eslint-disable-next-line no-console
        expect(console.debug).not.toHaveBeenCalled();
        // eslint-disable-next-line no-console
        expect(console.info).not.toHaveBeenCalled();
        // eslint-disable-next-line no-console
        expect(console.warn).not.toHaveBeenCalled();
        // eslint-disable-next-line no-console
        expect(console.error).not.toHaveBeenCalled();

        expect(logging.child({})).toBe(logging);
    });
});
