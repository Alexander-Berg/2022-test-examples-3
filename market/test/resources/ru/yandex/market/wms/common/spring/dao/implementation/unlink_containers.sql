-- H2 compatible SQL
delete from LOCxID where ID in (select id from (values %s) as containers(id))
