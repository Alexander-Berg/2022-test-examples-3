import { debounce } from '../debounce';

/** @todo https://st.yandex-team.ru/MSSNGRFRONT-2319 */
describe.skip('Debounce', () => {
    describe('#debounce', () => {
        it('Should call', (done) => {
            debounce(done, 20)();
        }, 30);

        it('Should call immediately when leading', (done) => {
            debounce(done, 20, true)();
        }, 10);

        it('Should call once when delay < call interval', (done) => {
            let counter = 0;

            const call = debounce(() => {
                counter++;
            }, 15);

            setTimeout(call, 10);
            setTimeout(call, 20);
            setTimeout(call, 30);

            setTimeout(() => {
                expect(counter).toEqual(1);
                done();
            }, 60);
        }, 80);

        it('Should call once when delay < call interval and leading', (done) => {
            let counter = 0;

            const call = debounce(() => {
                counter++;
            }, 15, true);

            setTimeout(call, 10);
            setTimeout(call, 20);
            setTimeout(call, 30);

            setTimeout(() => {
                expect(counter).toEqual(1);
                done();
            }, 60);
        }, 80);

        it('Should call every time when delay > call interval', (done) => {
            let counter = 0;

            const call = debounce(() => {
                counter++;
            }, 15);

            setTimeout(call, 10);
            setTimeout(call, 30);
            setTimeout(call, 50);

            setTimeout(() => {
                expect(counter).toEqual(3);
                done();
            }, 80);
        }, 100);
    });
});
