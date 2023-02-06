truncate public.installment_files cascade;

insert into public.installment_files(resource_id, shop_id, business_id, name, correct_selected_offers,
                                     invalid_offers, status, total_offers, url_to_download, file_type)
values ('de7cd822-d5f9-47cb-8fee-36546d7c9ab1', 1, 2, 'file.xlsx', 0, 0, 'PENDING', 10, 'url', 'INSTALLMENT');
