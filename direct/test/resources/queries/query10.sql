UPDATE managers m
INNER JOIN 
(
    SELECT /* method:getPetrovs */ 
        id , name,family, 
            exists(select z, x, y from coords where coods.user_id = id)
       FROM users -- get all Petrovs and manager_id in ( some ids )
       WHERE
            family="Petrov" and
            (
                group_id =-1 or manager_id in ( "1", "3", "8" , "92" )
            )
       and salary>-20e-15
    ) AS b ON a.name_a = b.name_b
SET validation_check = if(start_dts > end_dts, 'VALID', ''),
family=b.family
where m.id = 100;
