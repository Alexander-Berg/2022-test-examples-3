INSERT INTO dbo.vperfomance_history4 (whs,operation_group, "Сотрудник", "Staff/OutStaff", "Новый сотрудник",
                                      "Администратор", "Ячейка", d1, "Количество", "Время", ye, ye_abs, "Шт/час", "Час", "Смена",
                                      "YЕ за смену", "Статус эффективности", "Опер.день")
VALUES ('SOF',N'Другое', N'test-with-null', N'Staff', N'Новый сотрудник, менее 2 нед.', N'Кладовщик',
        null, null, 9, 9, 0, 150.64, 9, N'2021-09-15 10:00', N'дневная смена', N'0.16',
        N'Не эффективная работа', N'2021-09-15');
