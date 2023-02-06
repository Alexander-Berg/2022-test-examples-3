const { ReactEntity } = require('../../../../vendors/hermione');

const elems = {};

elems.timer = new ReactEntity({ block: 'Timer' });
elems.timerFullscreen = elems.timer.mods({ fullscreen: true });

elems.timer.hoursInput = new ReactEntity({ block: 'Timer', elem: 'Input' }).nthChild(1);
elems.timer.hoursInput.control = new ReactEntity({ block: 'Textinput', elem: 'Control' });

elems.timer.minutesInput = new ReactEntity({ block: 'Timer', elem: 'Input' }).nthChild(2);
elems.timer.minutesInput.control = new ReactEntity({ block: 'Textinput', elem: 'Control' });

elems.timer.secondsInput = new ReactEntity({ block: 'Timer', elem: 'Input' }).nthChild(3);
elems.timer.secondsInput.control = new ReactEntity({ block: 'Textinput', elem: 'Control' });

elems.timer.controls = new ReactEntity({ block: 'Timer', elem: 'Controls' });
elems.timer.controls.actionButton = new ReactEntity({ block: 'Button2' }).mods({ view: 'action' });
elems.timer.controls.defaultButton = new ReactEntity({ block: 'Button2' }).mods({ view: 'default' });
elems.timer.controls.clearButton = new ReactEntity({ block: 'Button2' }).mods({ view: 'clear' });

elems.timer.fullscreenEnterButton = new ReactEntity({ block: 'Timer', elem: 'FullscreenEnterButton' });
elems.timer.fullscreenExitButton = new ReactEntity({ block: 'Timer', elem: 'FullscreenExitButton' });

elems.timer.controlsFullscreen = new ReactEntity({ block: 'Timer', elem: 'ControlsFullscreen' });
elems.timer.controlsFullscreen.left = new ReactEntity({ block: 'Timer', elem: 'ControlsFullscreenLeft' });
elems.timer.controlsFullscreen.left.escButton = new ReactEntity({ block: 'Button2' });
elems.timer.controlsFullscreen.right = new ReactEntity({ block: 'Timer', elem: 'ControlsFullscreenRight' });
elems.timer.controlsFullscreen.right.spaceButton = new ReactEntity({ block: 'Button2' });
elems.timer.fullscreenLogo = new ReactEntity({ block: 'Timer', elem: 'FullscreenLogo' });

module.exports = elems;
