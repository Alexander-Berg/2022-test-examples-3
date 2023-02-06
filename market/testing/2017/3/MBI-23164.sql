--liquibase formatted sql

--changeset snoop:MBI-23164-remove_invalid_bids
DELETE FROM shops_web.auction_offer_bid WHERE LENGTH(offer_name) > 512;

