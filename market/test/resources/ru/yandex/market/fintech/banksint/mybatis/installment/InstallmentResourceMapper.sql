truncate public.installment_files CASCADE;
INSERT INTO public.installment_files(resource_id, shop_id, business_id, name, correct_selected_offers,
                                     invalid_offers, status, total_offers, url_to_download, file_type,
                                     updated_at, fail_reason)
VALUES ('691634f8-3ab9-4549-a7b8-53147335eff9', 42, 2, 'file1.xlsx', 0, 0, 'PENDING', 10, 'url1', 'TEMPLATE',
        '2021-10-19 15:18:50.000000', null),
       ('c02c6b8c-936d-4373-8a7d-bd8bbe8aa72d', 42, 2, 'file2.xlsx', 0, 0, 'PROCESSING', 10, 'url2', 'TEMPLATE',
        '2021-10-19 15:18:50.000000', null),
       ('de7cd822-d5f9-47cb-8fee-36546d7c9ab1', 42, 2, 'file3.xlsx', 0, 0, 'PENDING', 0, 'url3', 'INSTALLMENT',
        '2021-10-19 15:18:50.000000', null),
       ('85b438fc-a482-4fd7-83d1-505c6b5dcc30', 42, 2, 'file4.xlsx', 0, 0, 'PROCESSING', 0, 'url4', 'INSTALLMENT',
        '2021-10-19 15:18:50.000000', null),
       ('319021fd-a7d7-45d0-837d-82a8e4c4bce0', 42, 2, 'file5.xlsx', 10, 0, 'DONE', 10, 'url5', 'INSTALLMENT',
        '2021-10-19 15:18:50.000000', null),
       ('cb929885-0805-41c4-b681-62b74d6ff90a', 42, 2, 'file6.xlsx', 0, 10, 'FAILED', 10, 'url6', 'INSTALLMENT',
        '2021-10-19 15:18:50.000000', 'критическая ошибка валидации файла'),
       ('60ffd4cc-c84a-43a1-b290-1a9eee36e124', 42, 2, 'file7.xlsx', 0, 10, 'DONE', 10, 'url7', 'INSTALLMENT',
        '2021-10-19 15:18:50.000000', null);



