var dataStub = require('../data')();

dataStub[0].context.push(
    {
        full_url: '//news.yandex.ru/yandsearch?rpt=nnews2&amp;grhow=clutop&amp;text=',
        is_best: '0',
        is_current: '0',
        name: 'news'
    }
);

module.exports = {
    type: 'navwizard',
    data_stub: dataStub
};