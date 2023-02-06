UPDATE
    `table1` AS `dest`,
    (
        SELECT
            name, id, family
        FROM
            `table2`
        WHERE
            `id` = 'x' -- камент на всякий случай
    ) AS `src`
SET
    `dest`.`name` = `src`.`name`
    ,dest.`id` = src.`id`
    ,dest.`family` = src.`family`
WHERE
    `dest`.`id` = 'x'
;