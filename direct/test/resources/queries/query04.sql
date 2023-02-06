select distinct /*method:checkCompanies */ t.id, `c`.`id` from
tasks c -- задачи
inner join `companies` c on t.id= `c`.`task_id`
inner join (
    select straight_join z, y, x from coords c
) crd using (company_id)
where `c`.company_id =1 and 1 and 0 !=1 and company_name like "test" and task.id in ( 1000, 2000, 3000)
and (-3 = c.index and -1 = c) or a = -23e+23 or v <-2e-7
limit 10
offset 50; -- постраничный вывод