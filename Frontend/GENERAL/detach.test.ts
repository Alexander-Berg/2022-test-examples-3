import { delay } from './time';
import { addDetachListener, delDetachListener } from './detach';

describe('detach', () => {
    it('Should add detach listener', async() => {
        const elem1 = document.createElement('div');
        const elem2 = document.createElement('div');

        document.body.appendChild(elem1);
        document.body.appendChild(elem2);

        expect(elem1.isConnected).toBe(true);
        expect(elem2.isConnected).toBe(true);

        const spy = jest.fn();

        addDetachListener(elem1, () => spy());
        addDetachListener(elem1, () => spy());
        addDetachListener(elem2, () => spy());
        addDetachListener(elem2, () => spy());

        document.body.removeChild(elem1);
        document.body.removeChild(elem2);

        await delay(100);

        expect(elem1.isConnected).toBe(false);
        expect(elem2.isConnected).toBe(false);

        expect(spy).toHaveBeenCalledTimes(4);
    });

    it('Should del detach listener', async() => {
        const elem1 = document.createElement('div');
        const elem2 = document.createElement('div');

        document.body.appendChild(elem1);
        document.body.appendChild(elem2);

        expect(elem1.isConnected).toBe(true);
        expect(elem2.isConnected).toBe(true);

        const spy1 = jest.fn();
        const spy2 = jest.fn();

        addDetachListener(elem1, spy1);
        addDetachListener(elem1, spy2);

        delDetachListener(elem1, spy1);
        delDetachListener(elem1, spy2);

        // should not fail
        delDetachListener(elem2, spy1);
        delDetachListener(elem2, spy2);

        document.body.removeChild(elem1);
        document.body.removeChild(elem2);

        await delay(100);

        expect(elem1.isConnected).toBe(false);
        expect(elem2.isConnected).toBe(false);

        expect(spy1).toHaveBeenCalledTimes(0);
        expect(spy2).toHaveBeenCalledTimes(0);
    });
});
