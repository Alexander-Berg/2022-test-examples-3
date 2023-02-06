CREATE SCHEMA site_catalog;
CREATE SCHEMA market_content;

CREATE TABLE market_content.mc_category (
  hyper_id bigint NOT NULL,
  CONSTRAINT "pk_category_hid" PRIMARY KEY (hyper_id)
);

CREATE TABLE site_catalog.sc_vendor (
  id bigint NOT NULL,
  CONSTRAINT "pk_vendor_id" PRIMARY KEY (id)
);

CREATE TABLE market_content.category_favorite_vendor (
  category_hid bigint NOT NULL,
  vendor_id bigint NOT NULL,

  CONSTRAINT "pk_category_favorite_vendor" PRIMARY KEY (category_hid, vendor_id),

  CONSTRAINT fk_category FOREIGN KEY (category_hid)
    REFERENCES market_content.mc_category(hyper_id)
    ON DELETE CASCADE
);

SET SCHEMA market_content;
