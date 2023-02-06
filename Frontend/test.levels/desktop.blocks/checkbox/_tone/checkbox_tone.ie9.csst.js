module.exports = x =>
    `.utilityfocus .checkbox_view_default.checkbox_tone_${x.tone}.checkbox_focused_yes .checkbox__box:before {
    outline: 2px solid ${x['--color-border-focused']};
}`
;
