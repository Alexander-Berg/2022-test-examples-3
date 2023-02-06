describeBlock('advanced-search__lang', () => {
    describeBlock('advanced-search__prepare-filter-items', block => {
        it('should return items for checkboxes in the right order', () => {
            const { checkBoxItems: result } = block('com.tr');
            assert.deepEqual(result,
                [
                    { val: 'tr', text: 'Турецкий' },
                    { val: 'en', text: 'Английский' }
                ]
            );
        });

        it('should return items for selects in the right order', () => {
            const { selectItems: result } = block('ru');
            assert.deepEqual(result,
                [
                    { val: 'be', text: 'Белорусский' },
                    { val: 'id', text: 'Индонезийский' },
                    { val: 'kk', text: 'Казахский' },
                    { val: 'de', text: 'Немецкий' },
                    { val: 'tt', text: 'Татарский' },
                    { val: 'tr', text: 'Турецкий' },
                    { val: 'uk', text: 'Украинский' },
                    { val: 'fr', text: 'Французский' }
                ]
            );
        });

        it('should return default value for unknown tld', () => {
            const result = block('sometld');
            assert.deepEqual(result, {
                checkBoxItems: [
                    { val: 'ru', text: 'Русский' },
                    { val: 'en', text: 'Английский' }
                ],
                selectItems: [
                    { val: 'be', text: 'Белорусский' },
                    { val: 'id', text: 'Индонезийский' },
                    { val: 'kk', text: 'Казахский' },
                    { val: 'de', text: 'Немецкий' },
                    { val: 'tt', text: 'Татарский' },
                    { val: 'tr', text: 'Турецкий' },
                    { val: 'uk', text: 'Украинский' },
                    { val: 'fr', text: 'Французский' }
                ]
            });
        });
    });
});
