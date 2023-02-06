const _ = require('lodash');

const getMultipartPollData = require('../../../../../src/server/data-adapters/nirvana/poll/multipart-poll-data');
const { MULTIPART_POLL_DATA_INPUT, MULTIPART_POLL_DATA_OUTPUT } = require('./fixtures');

describe('nirvana/poll/multipart-poll-data', function() {
    let experiment;

    beforeEach(function() {
        experiment = _.cloneDeep(MULTIPART_POLL_DATA_INPUT);
    });

    it('корректно конвертирует параметры опроса во входные данные для шаблона', function() {
        assert.deepEqual(
            getMultipartPollData(experiment.questionGroups),
            MULTIPART_POLL_DATA_OUTPUT,
        );
    });
});
