require('models/config/config');

describe('Config model', () => {
    beforeEach(function() {
        this.model = ns.Model.get('config');
    });

    afterEach(function() {
        this.model.destroy();
        delete this.model;
    });
});
