var dataStub = require('../data')();

dataStub[0].context.forEach(tab => {
   if(tab.full_url) {
       tab.full_url += 'test+request';
   }
});

module.exports = {
    type: 'navwizard',
    data_stub: dataStub
};
