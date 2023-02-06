--liquibase formatted sql

--changeset empyros:MARKETCHECKOUT-25267-1
INSERT INTO mn_property(name, value)
VALUES ('useExpressNotification', 'true');

--changeset empyros:MARKETCHECKOUT-25267-2
INSERT INTO mn_property(name, value)
VALUES ('useExpressImmediateNotification', 'true');

--changeset empyros:MARKETCHECKOUT-25267-3
INSERT INTO mn_property(name, value)
VALUES ('useCommonReturnNotificationTemplate', 'true');

--changeset empyros:MARKETCHECKOUT-25267-4
INSERT INTO mn_property(name, value)
VALUES ('replacePendingWithProcessingForMultiOrder', 'true');

--changeset empyros:MARKETCHECKOUT-25267-5
INSERT INTO mn_property(name, value)
VALUES ('aggregateReceiptPrintedEmail', 'true');

--changeset empyros:MARKETCHECKOUT-25267-6
INSERT INTO mn_property(name, value)
VALUES ('aggregateProcessingEmail', 'true');

--changeset empyros:MARKETCHECKOUT-25267-7
INSERT INTO mn_property(name, value)
VALUES ('shipmentDeadlineNoteEnabled', 'true');

--changeset empyros:MARKETCHECKOUT-25267-8
INSERT INTO mn_property(name, value)
VALUES ('mergeDeliveredAndCashbackPush', 'true');

--changeset empyros:MARKETCHECKOUT-25267-9
INSERT INTO mn_property(name, value)
VALUES ('enableUserChangedMindConfirmation', 'true');

--changeset empyros:MARKETCHECKOUT-25267-10
INSERT INTO mn_property(name, value)
VALUES ('onlyUpdateBarriersOnCancelling', 'true');

--changeset empyros:MARKETCHECKOUT-25267-11
INSERT INTO mn_property(name, value)
VALUES ('beruPostamatReturnDeliveryServiceIds', '1005480');

--changeset empyros:MARKETCHECKOUT-25267-12
INSERT INTO mn_property(name, value)
VALUES ('smsSender', 'MARKET');
