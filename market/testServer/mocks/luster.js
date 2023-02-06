export const serverInit = new Promise(resolve => {
    const mockReady = () => {
        resolve();
    };
    jest.mock('luster', () => ({
        ready: mockReady,
    }));
});
