const { expect } = require('chai');

const xlsx = require('middleware/xlsx');
const Excel = require('models/excel');

describe('XLSX middleware', () => {
    it('should create XLSX from JSON', function *() {
        const context = {
            state: { blank: 'certificateBlank.xlsx' },
            body: {
                firstname: 'Mike',
                lastname: 'Smirnov',
                login: 'm-smirnov',
                role: 'User',
                examId: 2,
                examTitle: 'Direct',
                certId: 1,
                confirmedDate: new Date(),
                dueDate: new Date(),
                agencyLogin: 'm.smirnov',
                agencyManager: 'mokhov',
                isProctoring: 1,
                proctoringAnswer: 1,
                isMetricsHigh: 0,
                isPendingSentToToloka: 0,
                autoTolokaVerdict: '?',
                isRevisionRequested: 0,
                revisionVerdict: '?',
                appealVerdict: '?',
                finalVerdict: 1
            }
        };

        yield xlsx.call(context, {});

        const excel = Excel.tryLoad(context.body);

        expect(context.type).to.equal('application/vnd.openxmlformats-officedocument.spreadsheetml.sheet');
        expect(excel.worksheet['!ref']).to.equal('A1:X3');
    });

    it('should do nothing without blank', function *() {
        const context = {
            state: {},
            body: 'some value'
        };

        yield xlsx.call(context, {});

        expect(context.body).to.equal('some value');
    });
});
