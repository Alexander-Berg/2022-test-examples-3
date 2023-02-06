const El = require('../Entity');

const elems = {};

elems.Layout = new El({ block: 'Layout' });
elems.Button = new El({ block: 'Button2' });
elems.BillingIframe = new El({ block: 'BillingIframe' });
elems.MushroomsButton = new El({ block: 'MushroomsButton' });
elems.FirstMessengerButton = new El({ block: 'MessengerButton' }).nthChild(1);

// pages
elems.PlatformDoctorPage = new El({ block: 'PlatformDoctorPage' });
elems.CreateSession = new El({ block: 'CreateSession' });
elems.Billing = new El({ block: 'Billing' });

// modals
elems.DefaultModal = new El({ block: 'DefaultModal' });
elems.DefaultModalFullsize = elems.DefaultModal.mods({ fullsize: true });

const TopicSlider = 'TopicSlider';
elems.TopicSlider = new El({ block: TopicSlider });
elems.TopicSlider.Controls = new El({ block: TopicSlider, elem: 'Controls' });
elems.TopicSlider.Controls.SecondButton = elems.Button.copy().nthChild(2);
elems.TopicSlider.SliderCard = new El({ block: TopicSlider, elem: 'SliderCard' });
elems.TopicSlider.SliderCard.FirstTopicCard = new El({ block: 'TopicCard' }).nthChild(1);

const MushroomsRubricMenu = 'MushroomsRubricMenu';
elems.MushroomsRubricMenu = new El({ block: MushroomsRubricMenu });
elems.MushroomsRubricMenu.FirstNavItem = new El({ block: MushroomsRubricMenu, elem: 'NavItem' }).nthChild(1);

elems.FirstDoctorCard = new El({ block: 'DoctorCard' }).nthChild(1);

elems.SubscriptionCard = new El({ block: 'SubscriptionCard' });
elems.SubscriptionCard.FirstMushroomsButton = elems.MushroomsButton.copy().nthChild(1);

module.exports = elems;
