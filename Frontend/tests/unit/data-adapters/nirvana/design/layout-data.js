const { LAYOUTS_INPUT, LAYOUTS_OUTPUT } = require('./fixtures');
const getDesignData = require('../../../../../src/server/data-adapters/nirvana/design/layout-data');

describe('nirvana/layout-data', function() {
    it('должен правильно конвертировать коллекцию ссылок на макеты в формат макетного графа', function() {
        assert.deepEqual(getDesignData(LAYOUTS_INPUT), LAYOUTS_OUTPUT);
    });
    /*
    it('должен копировать общий вопрос в ноду каждого экрана без явно заданного вопроса', function() {
        const question = '???';
        exp.question = question;
        delete exp.layouts.screens[0].question;

        const { exp: freshedExp } = freshener(exp);

        assert.equal(freshedExp.layouts.screens[0].question, question);
    });

    it('должен копировать дефолтный вопрос в ноду каждого экрана, если для экрана не задан явно вопрос, не задан общий вопрос', function() {
        const question = 'Какой вариант вам больше нравится?';
        delete exp.question;
        delete exp.layouts.screens[0].question;

        const { exp: freshedExp } = freshener(exp);

        assert.equal(freshedExp.layouts.screens[0].question, question);
    });*/
});
