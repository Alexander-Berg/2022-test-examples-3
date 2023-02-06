--changeset dgir:MARKETCHECKOUT-22507_2
CREATE TABLE public.test_data_medicata (
                                           id int NOT NULL GENERATED ALWAYS AS IDENTITY,
                                           generic_name varchar(15) NULL,
                                           esklp_mnn varchar(41) NULL,
                                           guids varchar(300) NULL
);
CREATE INDEX test_data_medicata_generic_name_idx ON public.test_data_medicata (generic_name);
CREATE INDEX test_data_medicata_esklp_mnn_idx ON public.test_data_medicata (esklp_mnn);
