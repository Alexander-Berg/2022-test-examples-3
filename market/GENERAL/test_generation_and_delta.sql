--liquibase formatted sql
--changeset ogonek:MBI-31489(2)-add-is-imported-column-to-test-generation-and-delta

ALTER TABLE SHOPS_WEB.TEST_GENERATION_AND_DELTA ADD (is_imported NUMBER(1) DEFAULT 1 NOT NULL);

--changeset ogonek:MBI-31739-delete-is-imported-column-drom-test-generation-and-delta
ALTER TABLE SHOPS_WEB.TEST_GENERATION_AND_DELTA DROP COLUMN is_imported;

--changeset ogonek:MBI-31811-add-is-imported-column-to-test-generation-and-delta
ALTER TABLE SHOPS_WEB.TEST_GENERATION_AND_DELTA ADD (is_imported NUMBER(1) DEFAULT 1 NOT NULL);

--changeset dpastukhov:MBI-38308-add-unique-index-for-test-generation-name
create unique index SHOPS_WEB.UI_TEST_GNR_NAME on SHOPS_WEB.TEST_GENERATION_AND_DELTA (case when TYPE = 0 then NAME else null end);