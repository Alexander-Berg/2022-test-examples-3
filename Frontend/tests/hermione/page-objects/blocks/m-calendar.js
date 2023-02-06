const Entity = require('bem-page-object').Entity;

const mCalendarPopup = new Entity({ block: 'm-calendar', elem: 'popup' }).mix(
    new Entity({ block: 'popup2' }).mods({ visible: 'yes' })
);
mCalendarPopup.datepicker = new Entity({ block: 'm-datepicker' });
mCalendarPopup.datepicker.title = new Entity({ block: 'm-datepicker', elem: 'title-click' });
mCalendarPopup.datepicker.year = new Entity({ block: 'm-datepicker', elem: 'choose-year' });
mCalendarPopup.datepicker.year.input = new Entity({ block: 'input', elem: 'control' });
mCalendarPopup.datepicker.months = new Entity({ block: 'm-datepicker', elem: 'choose-month' });
mCalendarPopup.datepicker.months.item = new Entity({ block: 'radio-button', elem: 'radio' });
mCalendarPopup.datepicker.ok = new Entity({ block: 'm-datepicker', elem: 'chooser-submit' });
mCalendarPopup.datepicker.ok.btn = new Entity({ block: 'button' });
mCalendarPopup.datepicker.dates = new Entity({ block: 'm-datepicker', elem: 'dates' });

module.exports = mCalendarPopup;
