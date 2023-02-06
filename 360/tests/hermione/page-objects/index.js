const { Entity } = require('@yandex-int/bem-page-object');

const PO = {};

PO.user = new Entity('.User');
PO.copyright = new Entity('.Copyright');
PO.chatWidget = new Entity('.ya-chat-widget.ya-chat-widget_desktop');
PO.errorStack = new Entity('.ErrorStack');
PO.nextBuildWatcher = new Entity('#__next-build-watcher');

/**
 * Orders
 */
PO.orders = new Entity('.Bills');
PO.orders.createOrderButton = new Entity('.js-create-order');
PO.orders.order = new Entity('.Bill');
PO.orders.openedOrder = new Entity('.Bill__opened');
PO.orders.firstOrder = PO.orders.order.nthChild(1);
PO.orders.firstOpenedOrder = PO.orders.firstOrder.mix(PO.orders.openedOrder);
PO.orders.order.mainBlock = new Entity('.Bill__mainBlock');
PO.orders.order.menu = new Entity('.BillMenu');
PO.orders.order.menu.createRefundButton = new Entity('.js-create-refund');
PO.orders.order.menu.deactivateOrderButton = new Entity('.js-deactivate-order');
PO.orders.order.activateOrderButton = new Entity('.js-activate-order');
PO.orders.order.subtitle = new Entity('.Bill__subtitle');
PO.orders.order.date = new Entity('.Bill__dateWrapper');
PO.orders.orderCreation = new Entity('.CreateBill');
PO.orders.orderCreation.body = new Entity('.CreateBill__body');
PO.orders.orderCreation.captionInput = new Entity('#caption');
PO.orders.orderCreation.item = new Entity('.CreateBill__item');
PO.orders.orderCreation.item.nameInput = new Entity('#name');
PO.orders.orderCreation.item.amountInput = new Entity('#amount');
PO.orders.orderCreation.item.priceInput = new Entity('#price');
PO.orders.orderCreation.item.deleteItemButton = new Entity(
  '.CreateBill__delete'
);
PO.orders.orderCreation.addItemButton = new Entity('.CreateBill__add');
PO.orders.orderCreation.startOrderCreationButton = new Entity(
  '.js-start-order-creation'
);
PO.orders.orderCreation.cancelOrderCreationButton = new Entity(
  '.js-cancel-order-creation'
);

/**
 * Refund
 */
PO.refund = new Entity('.Refund');
PO.refund.id = new Entity('.RefundHeader-Id');
PO.refund.item = new Entity('.RefundItems__item');
PO.refund.item.checkbox = new Entity('.RefundItem__checkbox');
PO.refund.item.name = new Entity('.RefundItem__name');
PO.refund.item.price = new Entity('.RefundItem__price');
PO.refund.item.amountInput = new Entity('.RefundItem__amount input');
PO.refund.startRefundCreationButton = new Entity('.js-start-refund-creation');
PO.refund.cancelRefundCreationButton = new Entity('.js-cancel-refund-creation');

/**
 * Registration
 */
PO.registration = {};

/**
 * Registration legal
 */

PO.registration.initial = new Entity('.RegistrationLayout-Main');
PO.registration.initial.yandexBusinessOption = new Entity(
  '.ServiceSelectGrid-Option[aria-disabled="false"]'
).nthChild(2);
PO.registration.initial.innInput = new Entity(
  'input[id="field_inn"]'
);
PO.registration.initial.caregotyInput = new Entity(
  'input[id="field_categories"]'
);
PO.registration.initial.suggestForInputs = new Entity(
  '.Popup2_visible li'
);
PO.registration.initial.enabledButton = new Entity(
  '.Row-Item button[aria-disabled="false"]'
);

/**
 * Registration legal
 */
PO.registration.legal = new Entity('.LegalPageContent');
PO.registration.legal.organizationIPTypeCheckbox = new Entity(
  'input[name="organization.type"][value="ip"]'
);
PO.registration.legal.organizationOOOTypeCheckbox = new Entity(
  'input[name="organization.type"][value="ooo"]'
);
PO.registration.legal.organizationNameInput = new Entity(
  'input[name="organization.name"]'
);
PO.registration.legal.organizationFullNameInput = new Entity(
  'input[name="organization.fullName"]'
);
PO.registration.legal.organizationEnglishNameInput = new Entity(
  'input[name="organization.englishName"]'
);
PO.registration.legal.organizationOGRNInput = new Entity(
  'input[name="organization.ogrn"]'
);
PO.registration.legal.organizationINNInput = new Entity(
  'input[name="organization.inn"]'
);
PO.registration.legal.organizationKPPInput = new Entity(
  'input[name="organization.kpp"]'
);
PO.registration.legal.organizationScheduleInput = new Entity(
  'input[name="organization.scheduleText"]'
);
PO.registration.legal.organizationDescriptionInput = new Entity(
  'input[name="organization.description"]'
);
PO.registration.legal.organizationSiteUrlInput = new Entity(
  'input[name="organization.siteUrl"]'
);
PO.registration.legal.ceoPersonSurnameInput = new Entity(
  'input[name="persons.ceo.surname"]'
);
PO.registration.legal.ceoPersonNameInput = new Entity(
  'input[name="persons.ceo.name"]'
);
PO.registration.legal.ceoPersonPatronymicInput = new Entity(
  'input[name="persons.ceo.patronymic"]'
);
PO.registration.legal.ceoPersonBirthDateInput = new Entity(
  'input[name="persons.ceo.birthDate"]'
);
PO.registration.legal.ceoPersonPhoneInput = new Entity(
  'input[name="persons.ceo.phone"]'
);
PO.registration.legal.ceoPersonEmailInput = new Entity(
  'input[name="persons.ceo.email"]'
);
PO.registration.legal.contactPersonSurnameInput = new Entity(
  'input[name="persons.contact.surname"]'
);
PO.registration.legal.contactPersonNameInput = new Entity(
  'input[name="persons.contact.name"]'
);
PO.registration.legal.contactPersonPatronymicInput = new Entity(
  'input[name="persons.contact.patronymic"]'
);
PO.registration.legal.contactPersonPhoneInput = new Entity(
  'input[name="persons.contact.phone"]'
);
PO.registration.legal.legalAddressZipInput = new Entity(
  'input[name="addresses.legal.zip"]'
);
PO.registration.legal.legalAddressCityInput = new Entity(
  'input[name="addresses.legal.city"]'
);
PO.registration.legal.legalAddressStreetInput = new Entity(
  'input[name="addresses.legal.street"]'
);
PO.registration.legal.legalAddressHomeInput = new Entity(
  'input[name="addresses.legal.home"]'
);
PO.registration.legal.postAddressZipInput = new Entity(
  'input[name="addresses.post.zip"]'
);
PO.registration.legal.postAddressCityInput = new Entity(
  'input[name="addresses.post.city"]'
);
PO.registration.legal.postAddressStreetInput = new Entity(
  'input[name="addresses.post.street"]'
);
PO.registration.legal.postAddressHomeInput = new Entity(
  'input[name="addresses.post.home"]'
);
PO.registration.legal.bankNameInput = new Entity('input[name="bank.name"]');
PO.registration.legal.bankCorrespondentAccountInput = new Entity(
  'input[name="bank.correspondentAccount"]'
);
PO.registration.legal.bankAccountInput = new Entity(
  'input[name="bank.account"]'
);
PO.registration.legal.bankBikInput = new Entity('input[name="bank.bik"]');
PO.registration.legal.withContactPersonCheckbox = new Entity(
  '.js-contact-person-checkbox'
);
PO.registration.legal.withPostAddressCheckbox = new Entity(
  '.js-post-address-checkbox'
);
PO.registration.legal.startLegalDataSavingButton = new Entity(
  '.js-start-legal-data-saving'
);

/**
 * Registration documents
 */
const attachGroup = new Entity('.AttachGroup');

PO.registration.documents = new Entity('.DocumentsPageContent');
PO.registration.documents.offerInput = new Entity('input[name="offer"]');
PO.registration.documents.offerAttachGroup = PO.registration.documents.offerInput.adjacentSibling(
  attachGroup
);
PO.registration.documents.passportInput = new Entity('input[name="passport"]');
PO.registration.documents.passportAttachGroup = PO.registration.documents.passportInput.adjacentSibling(
  attachGroup
);
PO.registration.documents.signerPassportInput = new Entity(
  'input[name="signer_passport"]'
);
PO.registration.documents.signerPassportAttachGroup = PO.registration.documents.signerPassportInput.adjacentSibling(
  attachGroup
);
PO.registration.documents.proxyInput = new Entity('input[name="proxy"]');
PO.registration.documents.proxyAttachGroup = PO.registration.documents.proxyInput.adjacentSibling(
  attachGroup
);
PO.registration.documents.otherInput = new Entity('input[name="other"]');
PO.registration.documents.otherAttachGroup = PO.registration.documents.otherInput.adjacentSibling(
  attachGroup
);
PO.registration.documents.withSignerDocumentsCheckbox = new Entity(
  '.js-signer-documents-checkbox'
);
PO.registration.documents.withOtherDocumentsCheckbox = new Entity(
  '.js-other-documents-checkbox'
);
PO.registration.documents.startModerationButton = new Entity(
  '.js-start-moderation'
);
PO.registration.documents.openLegalDataModalButton = new Entity(
  '.js-open-legal-data-modal'
);

/**
 * Legal modal
 */
PO.registration.legalModal = new Entity('.LegalModal');
PO.registration.legalModal.content = new Entity('.Modal-Content');
PO.registration.legalModal.changeLegalDataButton = new Entity(
  '.js-change-legal-data'
);

/**
 * Registration moderation ongoing
 */
PO.registration.moderationOngoing = new Entity('.ModerationOngoingPageContent');

/**
 * Payment
 */
PO.payment = new Entity('.payment__content');
PO.payment.emailInput = new Entity('input[name="email"]');
PO.payment.descriptionInput = new Entity('input[name="description"]');
PO.payment.startPaymentButton = new Entity('.js-start-payment');

/**
 * Payment error
 */
PO.paymentError = new Entity('.PaymentError');

module.exports = PO;
