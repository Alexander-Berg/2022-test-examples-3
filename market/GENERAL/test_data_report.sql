--changeset dgir:MARKETCHECKOUT-22507
CREATE TABLE public.test_data_report (
                                         offer_id varchar(50) NOT NULL,
                                         barcode varchar(14) NULL,
                                         esklp_mnn varchar(41) NULL
);
CREATE UNIQUE INDEX test_data_report_offer_id_idx ON public.test_data_report (offer_id);
