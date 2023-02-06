const Entity = require('bem-page-object').Entity;

const fEvent = new Entity({ block: 'f-event' });
fEvent.date = new Entity({ block: 'f-event', elem: 'date' });
fEvent.room = new Entity({ block: 'f-event', elem: 'room' });
fEvent.spin = new Entity({ block: 'spin2' });

module.exports = fEvent;
