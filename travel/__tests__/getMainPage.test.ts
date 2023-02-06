import getMainPage from '../getMainPage';

const expectedMainPage = {
    url: '/',
    name: 'Главная',
    title: 'Расписание пригородного и междугороднего транспорта',
};

describe('getMainPage', () => {
    it('Return correct main page object', () => {
        expect(getMainPage('/')).toEqual(expectedMainPage);
    });
});
