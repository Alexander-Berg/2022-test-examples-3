const Controller = require('../../../../src/server/controller');

class Results extends Controller {
    getStatusOrResults() {
        return Promise.resolve()
            .then(() => this.res.json({
                'stage': 'Готов к разметке',
                'status': 'Завершился успешно',
            }).end())
            .catch((err) => this.renderJsonError(err));
    }

    getWinAgainstControlSystem() {
        this.res.json({});
    }

    getQueriesAnalysis() {
        this.res.json({});
    }

    getScenarioAbResults() {
        this.res.json({});
    }
}

module.exports = Results;
