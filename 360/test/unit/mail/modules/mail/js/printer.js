describe('Daria.Printer', function() {

    beforeEach(function() {
        this.uid = '011235';

        this.sinon.stub(Daria, 'uid').value(this.uid);
    });

    describe('#printMessage', function() {

        beforeEach(function() {
            this.sinon.stub(window, 'open');
        });

        it('Должен запускать печать сообщения', function() {
            var mid = '123456';
            var expectedURL = window.location.origin + Daria.Printer.PRINT_URL + '?mid=' + mid + '&_uid=' + this.uid;

            Daria.Printer.printMessage(mid);

            expect(window.open).to.be.calledWith(expectedURL);
        });

    });

    describe('#printThread', function() {

        beforeEach(function() {
            this.sinon.stub(window, 'open');
        });

        it('Должен запускать печать треда', function() {
            var tid = 't123456';
            var expectedURL = window.location.origin + Daria.Printer.PRINT_URL + '?tid=' + tid + '&_uid=' + this.uid;

            Daria.Printer.printThread(tid);

            expect(window.open).to.be.calledWith(expectedURL);
        });

    });

});
