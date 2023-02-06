describe('b-banner-preview2', function() {
    describe('filterFlagsWithSameParent', function() {
        var sandbox,
            constStub;

        beforeEach(function() {
            sandbox = sinon.sandbox.create();
            constStub = sandbox.stub(u, 'consts');

            constStub.withArgs('AD_WARNINGS').returns({
                "med_services": {
                    "short_text": "мед. услуги",
                    "long_text": "Имеются противопоказания. Посоветуйтесь с врачом",
                    "parent": "medicine"
                },
                "pharmacy": {
                    "long_text": "Имеются противопоказания. Посоветуйтесь с врачом",
                    "short_text": "лекарства",
                    "parent": "medicine"
                },
                "abortion": {
                    "short_text": "аборты",
                    "long_text": "Есть противопоказания. Посоветуйтесь с врачом. Возможен вред здоровью."
                },
                "med_equipment": {
                    "long_text": "Имеются противопоказания. Посоветуйтесь с врачом",
                    "short_text": "мед. оборудование",
                    "parent": "medicine"
                },
                "age": {
                    "variants": [
                        18,
                        16,
                        12,
                        6,
                        0
                    ],
                    "default": 18,
                    "is_common_warn": true
                }
            });

            constStub.withArgs('rights').returns({});
        });

        afterEach(function() {
            sandbox.restore();
            constStub.restore();
        });

        it('Должен отфильтровывать флаги с одним и тем же родителем', function() {
            expect(u['b-banner-preview2'].filterFlagsWithSameParent({
                "med_services": 1,
                "medicine": 1,
                "med_equipment": 1,
                "pharmacy": 1,
                "age": "18"
            })).eql({ med_services: 1, medicine: 1, age: '18' });
        });

    });
});
