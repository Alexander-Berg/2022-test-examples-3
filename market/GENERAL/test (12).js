const path = require('path');

const testExecute = require('@yandex-levitan/codemods/utils/testExecute');

const groupTransform = require('../b2b-group.transform');
const buttonTransform = require('../b2b-button.transform');

describe('ToggleButtonGroup', () => {
    testExecute(
        test,
        expect,
        path.join(__dirname, 'cases/group/'),
        groupTransform,
    );
});

describe('ToggleButton', () => {
    testExecute(
        test,
        expect,
        path.join(__dirname, 'cases/button/'),
        buttonTransform,
    );
});
