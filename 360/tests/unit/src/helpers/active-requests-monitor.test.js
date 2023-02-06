import ActiveRequestsMonitor from '../../../../src/helpers/active-requests-monitor';

describe('src/helpers/active-requests-monitor =>', () => {
    describe('ActiveRequetsMonitor =>', () => {
        it('active request count should be equal to 10 unresolved promises', () => {
            for (let i = 0; i < 10; ++i) {
                ActiveRequestsMonitor.addRequest(new Promise((resolve) => {
                    resolve(true);
                }));
            }
            expect(ActiveRequestsMonitor.activeRequests).toBe(10);
            expect(ActiveRequestsMonitor.isRequestInProgress).toBe(true);
        });

        ['resolve', 'reject'].forEach((result) => {
            it(`active request count should be 0 when all promises ${result}`, async() => {
                const n = 10;
                const promises = [];

                for (let i = 0; i < n; ++i) {
                    const promise = Promise[result](true);

                    promises.push(promise);
                    ActiveRequestsMonitor.addRequest(promise);
                }

                expect(ActiveRequestsMonitor.activeRequests).toBe(n);

                await Promise.all(promises).catch(() => {});

                expect(ActiveRequestsMonitor.activeRequests).toBe(0);
            });
        });

        it('active request count should be 0 when all promises resolve/reject', async() => {
            const n = 10;
            const promises = [];

            const thenCallback = jest.fn();
            const catchCallback = jest.fn();

            for (let i = 0; i < n; ++i) {
                const promise = new Promise((resolve, reject) => {
                    if (i % 2 === 0) {
                        resolve(i);
                    } else {
                        reject(i);
                    }
                });

                promises.push(promise);
                ActiveRequestsMonitor.addRequest(promise);
            }

            expect(ActiveRequestsMonitor.activeRequests).toBe(n);

            for (let i = 0; i < n; i++) {
                await promises[i]
                    .then(() => thenCallback(i % 2 === 0))
                    .catch(() => catchCallback(i % 2 === 0));
            }
            expect(thenCallback).toHaveBeenCalledTimes(n / 2);
            expect(thenCallback).toHaveBeenCalledWith(true);
            expect(catchCallback).toHaveBeenCalledTimes(n / 2);
            expect(catchCallback).toHaveBeenCalledWith(false);

            expect(ActiveRequestsMonitor.activeRequests).toBe(0);
        });
    });
});
