module.exports = {
    type: 'wizard',
    request_text: 'серфинг -виндсерфинг',
    data_stub: [{
        AllWords: ['серфинг виндсерфинг'],
        DeletedWords: ['виндсерфинг'],
        DeletedWordsDocumentLevel: ['виндсерфинг'],
        applicable: 1,
        counter_prefix: '/wiz/minuswords/',
        remove: [
            'misspell_source'
        ],
        type: 'minuswords',
        types: {
            all: [
                'wizard',
                'minus_words',
                'generic'
            ],
            extra: [],
            kind: 'wizard',
            main: 'minus_words/generic'
        },
        wizplace: 'upper'
    }, {
        AllWords: ['серфинг виндсерфингвиндсерфингвиндсерфингвиндсерфингвиндсерфинг'],
        DeletedWords: ['виндсерфингвиндсерфингвиндсерфингвиндсерфингвиндсерфинг'],
        DeletedWordsDocumentLevel: ['виндсерфингвиндсерфингвиндсерфингвиндсерфингвиндсерфинг'],
        applicable: 1,
        counter_prefix: '/wiz/minuswords/',
        remove: [
            'misspell_source'
        ],
        type: 'minuswords',
        types: {
            all: [
                'wizard',
                'minus_words',
                'generic'
            ],
            extra: [],
            kind: 'wizard',
            main: 'minus_words/generic'
        },
        wizplace: 'upper'
    }]
};
