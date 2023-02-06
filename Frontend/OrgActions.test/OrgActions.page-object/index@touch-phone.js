const { ReactEntity } = require('../../../../../../vendors/hermione');
const { oneOrg, overlayOneOrg, overlayPanel } = require('../../../../Companies.test/Companies.page-object/index@touch-phone');
const { scroller } = require('../../../../../../components/Scroller/Scroller.test/Scroller.page-object/index@common');

const elems = {};

elems.RealtyPopup = new ReactEntity({ block: 'RealtyPopup' });
elems.RealtyPopup.Content = new ReactEntity({ block: 'Modal', elem: 'Content' });
elems.RealtyPopup.Phone = new ReactEntity({ block: 'RealtyPopup', elem: 'Phone' });
elems.ActionsButton = new ReactEntity({ block: 'OrgActionsButton' });

elems.oneOrg = oneOrg.copy();
elems.OrgActions = new ReactEntity({ block: 'OrgActions' });

elems.OrgActions.Scroller = scroller.copy();

elems.OrgActions.Item = new ReactEntity({ block: 'OrgActions', elem: 'Item' });
elems.OrgActions.Item.Text = new ReactEntity({ block: 'OrgActions', elem: 'ItemText' });
elems.OrgActions.Item.Button = elems.ActionsButton.copy();

elems.OrgActions.Call = elems.OrgActions.Item.mods({ type: 'call' });
elems.OrgActions.Contacts = elems.OrgActions.Item.mods({ type: 'contacts' });
elems.OrgActions.Sites = elems.OrgActions.Item.mods({ type: 'sites' });
elems.OrgActions.Route = elems.OrgActions.Item.mods({ type: 'route' });
elems.OrgActions.Delivery = elems.OrgActions.Item.mods({ type: 'delivery' });
elems.OrgActions.Booking = elems.OrgActions.Item.mods({ type: 'booking' });
elems.OrgActions.Share = elems.OrgActions.Item.mods({ type: 'share' });
elems.OrgActions.Action = new ReactEntity({ block: 'OrgActions', elem: 'Button' }).mods({ type: 'action' });

elems.BookingIframe = new ReactEntity({ block: 'OrgBookingButton', elem: 'Iframe' });

elems.oneOrg.OrgActions = elems.OrgActions.copy();

elems.overlayOneOrg = overlayOneOrg.copy();
elems.overlayOneOrg.OrgActions = elems.OrgActions.copy();

elems.overlayPanel = overlayPanel.copy();

module.exports = elems;
