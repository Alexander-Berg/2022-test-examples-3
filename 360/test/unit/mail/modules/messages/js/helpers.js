describe('Daria.messages.params4SimplePath', function() {

    describe('2pane ->', function() {

        beforeEach(function() {
            this.sinon.stub(Daria.Config, 'layout').value('2pane');
        });

        it('должен вернуть true для обычной папки', function() {
            expect(Daria.messages.params4SimplePath({
                current_folder: "2170000240000001064",
                threaded: "yes"
            })).to.be.equal(true);
        });

        it('должен вернуть false для письма', function() {
            expect(Daria.messages.params4SimplePath({
                ids: "2170000240000001064"
            })).to.be.equal(false);
        });

        it('должен вернуть true для треда', function() {
            expect(Daria.messages.params4SimplePath({
                thread_id: "t2170000000023535526",
                current_folder: "2170000240000001064",
                threaded: "yes"
            })).to.be.equal(true);
        });

        it('должен вернуть false для выборки по метке', function() {
            expect(Daria.messages.params4SimplePath({
                current_label: "2170000000004078294"
            })).to.be.equal(false);
        });

    });

    describe('3pane ->', function() {

        beforeEach(function() {
            this.sinon.stub(Daria.Config, 'layout').value('3pane-vertical');
        });

        it('должен вернуть true для обычной папки + тред', function() {
            expect(Daria.messages.params4SimplePath({
                current_folder: "2170000240000001064",
                thread_id: "t2170000000014062275",
                threaded: "yes"
            })).to.be.equal(true);
        });

        it('должен вернуть true для обычной папки + письма', function() {
            expect(Daria.messages.params4SimplePath({
                current_folder: "2170000240000001064",
                ids: "2170000000014062275",
                threaded: "yes"
            })).to.be.equal(true);
        });

        it('должен вернуть false для выборке по метке', function() {
            expect(Daria.messages.params4SimplePath({
                current_label: "2170000000004078294"
            })).to.be.equal(false);
        });

    });

});

describe('Daria.messages.params4SimpleLabel', function() {

    describe('2pane ->', function() {

        beforeEach(function() {
            this.sinon.stub(Daria.Config, 'layout').value('2pane');
        });

        it('должен вернуть true для обычной метки', function() {
            expect(Daria.messages.params4SimpleLabel({
                current_label: "2170000240000001064"
            })).to.be.equal(true);
        });

        it('должен вернуть true для метки Важные', function() {
            expect(Daria.messages.params4SimpleLabel({
                current_label: "2170000240000001064",
                important: "important"
            })).to.be.equal(true);
        });

        it('должен вернуть false для письма', function() {
            expect(Daria.messages.params4SimpleLabel({
                ids: "2170000240000001064"
            })).to.be.equal(false);
        });

        it('должен вернуть false для треда', function() {
            expect(Daria.messages.params4SimpleLabel({
                thread_id: "t2170000000023535526",
                current_label: "2170000240000001064"
            })).to.be.equal(false);
        });

        it('должен вернуть false для выборки по папке', function() {
            expect(Daria.messages.params4SimpleLabel({
                current_folder: "2170000000004078294"
            })).to.be.equal(false);
        });

    });

    describe('3pane ->', function() {

        beforeEach(function() {
            this.sinon.stub(Daria.Config, 'layout').value('3pane-vertical');
        });

        it('должен вернуть true для обычной папки + тред', function() {
            expect(Daria.messages.params4SimpleLabel({
                current_label: "2170000240000001064",
                thread_id: "t2170000000014062275"
            })).to.be.equal(true);
        });

        it('должен вернуть true для обычной папки + письма', function() {
            expect(Daria.messages.params4SimpleLabel({
                current_label: "2170000240000001064",
                ids: "2170000000014062275"
            })).to.be.equal(true);
        });

        it('должен вернуть false для выборке по метке', function() {
            expect(Daria.messages.params4SimpleLabel({
                current_folder: "2170000000004078294"
            })).to.be.equal(false);
        });

    });

});
