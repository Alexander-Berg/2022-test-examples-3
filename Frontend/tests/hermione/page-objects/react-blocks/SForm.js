const Entity = require('../Entity').ReactEntity;

const blocks = {};
const methods = {};

blocks.SField = new Entity({
    block: 'SField',
});

blocks.formError = new Entity({
    block: 'SValidationMessage',
}).mods({
    type: 'error',
});

blocks.SGridField = new Entity({
    block: 'SGrid',
    elem: 'Field',
});

blocks.gridRow = new Entity({
    block: 'SGrid',
    elem: 'Row',
});

blocks.gridAdd = new Entity({
    block: 'SGrid',
    elem: 'AddButton',
});

blocks.gridDelete = new Entity({
    block: 'SGrid',
    elem: 'DeleteButton',
});

methods.getSRow = name => new Entity({
    block: 'SRow',
}).mods({
    name,
});

methods.getSFieldOfName = name => blocks.SField.copy().mods({
    name,
});

methods.getSFieldOfType = type => blocks.SField.copy().mods({
    type,
});

methods.getSGroupOfType = type => {
    return new Entity({
        block: 'SGroup',
    }).mods({ type });
};

methods.getGridFieldInputOfName = name => {
    return blocks.SGridField.copy().mods({
        name,
    });
};

methods.getGridFieldComponentOfNameAndType = (name, type) => {
    return blocks.SGridField.copy().mods({
        name,
    }).descendant(new Entity({
        block: 'SField',
        elem: 'Component',
    }).mods({ type }));
};

module.exports = { blocks, methods };
