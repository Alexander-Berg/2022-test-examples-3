select distinct count(path) from Map where mapped_to is not null 



select id from Categories where not_used = 1

select id from Categories where not_used = 1 and id in (select distinct parent_id from Categories)

select id from Categories where not_used = 1 and id in (select distinct parent_id from Categories)




select count(1) from Categories as c where  not_used = 1 and  not exists (select distinct parent_id from Categories
where c.id=parent_id)

select distinct parent_id from Categories

select count(1) from Categories


-- ������ � ��� ���������


-- ��������� �� �������, � ������� ���� �������
select * from Categories where not_used = 1 and id in (select distinct parent_id from Categories)
and id in (select distinct mapped_to from Map where mapped_to is not null)

-- ������� ��������
select count(1) from Map where mapped_to in (select id from Categories where not_used = 1 and id in (select distinct parent_id from Categories))

-- ��������� �������, � ������� ���� �������
select count(1) from Categories c where not_used = 1 and not exists (select distinct parent_id from Categories where c.id=parent_id)
and id in (select distinct mapped_to from Map where mapped_to is not null)



-- ������� ��������
select count(1) from Map where mapped_to in (select id from Categories c where not_used = 1 and not exists (select distinct parent_id from Categories where c.id=parent_id))


-- ������� ��������� ������
select count(1) from Map