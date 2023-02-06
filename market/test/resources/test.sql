--test1
select * from test1

-- test2
select
*
from
test2

-- test3    
-- comment
-- commnet2
select *
  from test3

-- test_params1
select * from ${param1}

-- test_params2
select ${p1}, ${p1}, ${p3}, ${p2} from ${p1}, $${p2} order by ${p2}}${p3}
