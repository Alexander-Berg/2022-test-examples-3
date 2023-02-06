UPDATE tableA a -- группируем с table2
INNER JOIN 
(
        SELECT
            name, id, family
        FROM
            `table2`
    ) AS b ON a.name_a = b.name_b
SET validation_check = if(start_dts > end_dts, 'VALID', ''),
family=b.family