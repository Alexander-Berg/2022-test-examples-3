const yastatic = require('../services/yastatic');

it('Yastatic', () => {
    const data = yastatic(['test']);

    expect(data.test.length).toEqual(1);
    expect(data.test.includes('yastatic.net')).toBeTruthy();
});
