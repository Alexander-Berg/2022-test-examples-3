module.exports = x =>
    `.button2.button2_view_default.button2_tone_${x.tone}.button2_theme_action.button2_progress_yes:before,
.button2.button2_view_default.button2_tone_${x.tone}.button2_action_yes.button2_progress_yes:before {
    background-color: ${x['--color-bg-progress']};
    background-image: repeating-linear-gradient(-45deg, ${x['--color-bg-progress']}, ${x['--color-bg-progress']} 4px, ${x['--color-gradient-progress']} 4px, ${x['--color-gradient-progress']} 8px);
}`
;
