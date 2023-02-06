module.exports = function addReactSuggestValue(options) {
    const {
        block,
        text,
        position,
        clickToFocus = false,
    } = options;

    /*
     Если нет фокуса на поле ввода, может возникнуть баг с сокрытием модального окна, при выборе опции из саджеста
    */
    if (clickToFocus) {
        this.click(`${block} .ToolsSuggest`);
    }

    return this
        .pause(300)
        .setValue(`${block} .ToolsSuggest .Textinput input`, text)
        .pause(300)
        .waitForVisible('.Popup2_visible.ToolsSuggest-Popup .ToolsSuggest-Choices')
        .waitForHidden('.Popup2_visible.ToolsSuggest-Popup .ToolsSuggest-Empty')
        .waitForVisible(`.Popup2_visible.ToolsSuggest-Popup .ToolsSuggest-Choice:nth-child(${position})`)
        .click(`.Popup2_visible.ToolsSuggest-Popup .ToolsSuggest-Choice:nth-child(${position})`)
        .waitForHidden('.Popup2_visible.ToolsSuggest-Popup .ToolsSuggest-Choices')
        .pause(300);
};
