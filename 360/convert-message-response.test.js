'use strict';

const aiMock = require('../../../test/mock/ai.json');

const { makeMessage } = require('./convert-message-response.js');
const envelopesWithoutFrom = require('../../../test/mock/meta/envelopes-without-from.json');
const envelopesWithoutSubject = require('../../../test/mock/meta/envelopes-without-subject.json');
const filteredLabels = require('../../../test/mock/filtered-labels');

let core;

describe('#makeMessage', function() {
    beforeEach(() => {
        core = {
            auth: {
                get: jest.fn().mockReturnValue(aiMock)
            },
            params: {}
        };
    });

    it('устойчива к отсутствию from', () => {
        const res = envelopesWithoutFrom.map((item) => makeMessage(core, item, { labels: filteredLabels }));

        expect(res).toMatchSnapshot();
    });

    it('умеет в "без темы"', () => {
        const res = envelopesWithoutSubject.map((item) => makeMessage(core, item, { labels: filteredLabels }));

        expect(res).toMatchSnapshot();
    });
});
