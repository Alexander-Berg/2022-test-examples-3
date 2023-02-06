const { RuleTester } = require('eslint');
const noStoreOutsideContainer = require('../../lib/no-store-outside-container');

const ruleTester = new RuleTester({
    parser: require.resolve('@typescript-eslint/parser'),
});

ruleTester.run('no-store-outside-container', noStoreOutsideContainer, {
    valid: [
        { // connect внутри контейнера
            code: 'let justCode = testFunction();\n\texport let ConnectedApplication = connect(mapStateToProps)(Application);',
            filename: 'frontend/services/staff/myContainer.ts',
        },
        { // useDispatch внутри контейнера
            code: 'let dispatch = useDispatch();\n\tlet justCode = testFunction();',
            filename: 'frontend/services/staff/myContainer.ts',
        },
        { // useSelector внутри контейнера
            code: 'const id = useSelector((state: AppState) => state.pageData.persons?.filterId);',
            filename: 'frontend/services/staff/myContainer.ts',
        },
        { //connect, useSelector и useDispatch внутри контейнера
            code: `let dispatch = useDispatch();\n\texport let ConnectedApplication = connect(mapStateToProps)(Application);
            \tconst id = useSelector((state: AppState) => state.pageData.persons?.filterId);`,
            filename: 'frontend/services/staff/myContainer.ts',
        },
        { //код без connect и useDispatch внутри контейнера
            code: 'let justCode = testFunction();',
            filename: 'frontend/services/staff/myContainer.ts',
        },
        { // код без connect и useDispatch внутри файла, не заканчивающегося на Container
            code: 'let justCode = testFunction();',
            filename: 'frontend/services/staff/notContainerTest.ts',
        },
    ],

    invalid: [
        { // connect внутри файла, не заканчивающегося на Container
            code: 'let justCode = testFunction();\n\texport let ConnectedApplication = connect(mapStateToProps)(Application);',
            filename: 'frontend/services/staff/notContainerTest.ts',
            errors: [{ messageId: 'CONNECT_OUTSIDE_CONTAINER' }],
        },
        { // useDispatch внутри файла, не заканчивающегося на Container
            code: 'let dispatch = useDispatch();\n\tlet justCode = testFunction();',
            filename: 'frontend/services/staff/notContainerTest.ts',
            errors: [{ messageId: 'USE_DISPATCH_OUTSIDE_CONTAINER' }],
        },
        { // useSelector внутри файла, не заканчивающегося на Container
            code: 'const id = useSelector((state: AppState) => state.pageData.persons?.filterId);',
            filename: 'frontend/services/staff/notContainerTest.ts',
            errors: [{ messageId: 'USE_SELECTOR_OUTSIDE_CONTAINER' }],
        },
        { // connect и useDispatch внутри файла, не заканчивающегося на Container
            code: `let dispatch = useDispatch();\n\texport let ConnectedApplication = connect(mapStateToProps)(Application);
            \tconst id = useSelector((state: AppState) => state.pageData.persons?.filterId);`,
            filename: 'frontend/services/staff/notContainerTest.ts',
            errors: [
                { messageId: 'USE_DISPATCH_OUTSIDE_CONTAINER' },
                { messageId: 'CONNECT_OUTSIDE_CONTAINER' },
                { messageId: 'USE_SELECTOR_OUTSIDE_CONTAINER' },
            ],
        },
    ],
});
