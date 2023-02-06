truncate public.installment_files CASCADE;
INSERT INTO public.installment_files(resource_id, shop_id, business_id, name, correct_selected_offers,
                                     invalid_offers, status, total_offers, url_to_download, file_type,
                                     updated_at, fail_reason)
VALUES
       ('de7cd822-d5f9-47cb-8fee-36546d7c9ab1', 42, 2, 'file3.xlsx', 0, 0, 'PENDING', 0, 'url3', 'INSTALLMENT',
        now() - INTERVAL '2 min', null),
       ('85b438fc-a482-4fd7-83d1-505c6b5dcc30', 42, 2, 'file4.xlsx', 0, 0, 'PROCESSING', 0, 'url4', 'INSTALLMENT',
        now() - INTERVAL '6 min', null),

       ('13a36308-41e3-49a3-8ee4-db70eda3443f', 42, 2, 'file3.xlsx', 0, 0, 'PENDING', 0, 'url3', 'INSTALLMENT',
        now() - INTERVAL '30 sec', null),
       ('62e53663-9467-4eef-b3a6-86403d04c819', 42, 2, 'file4.xlsx', 0, 0, 'PROCESSING', 0, 'url4', 'INSTALLMENT',
        now() - INTERVAL '2 min', null),

       ('fb581a45-1b77-4359-950e-181f2c66143f', 42, 2, 'file3.xlsx', 0, 0, 'PENDING', 0, 'url3', 'INSTALLMENT',
        now() - INTERVAL '5 hours', null),
       ('2d1edb6a-8124-4e35-87e2-f4fe7b54d2ba', 42, 2, 'file4.xlsx', 0, 0, 'PROCESSING', 0, 'url4', 'INSTALLMENT',
        now() - INTERVAL '2 hours', null),
       ('cb929885-0805-41c4-b681-62b74d6ff90a', 42, 2, 'file6.xlsx', 0, 10, 'FAILED', 10, 'url6', 'INSTALLMENT',
        '2021-10-19 15:18:50.000000', 'критическая ошибка валидации файла'),
       ('60ffd4cc-c84a-43a1-b290-1a9eee36e124', 42, 2, 'file7.xlsx', 0, 10, 'DONE', 10, 'url7', 'INSTALLMENT',
        '2021-10-19 15:18:50.000000', null);



