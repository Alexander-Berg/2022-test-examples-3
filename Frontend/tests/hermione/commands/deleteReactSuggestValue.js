module.exports = function deleteReactSuggestValue(options) {
    const {
        formHeader,
        block,
        position,
    } = options;

    return this
        .click(`${block} .ToolsSuggest-Chosen:nth-child(${position}) .ToolsSuggest-ChosenRemove`)
        .click(formHeader);
        //Клик на 'body' вызывал багу - поле под block становилось активным, хотя саджест скрывался.
        //Лучше нажимать на текст (например заголовок формы), он ничего не делает, с ним этой ошибоки нет и саджест block'а скрывается
};
