create view v_uc_session_report as
select to_char(started_time,'yyyy-mm-dd hh24') as st_time,to_char(finished_time,'yyyy-mm-dd hh24') as fn_time,good,allx
from (
  select sum(case when http_status in (200,302,301) then 1 else 0 end) as good, count(1) as allx , session_id
  from uc_net_check
  where session_id is not null
  group by session_id
) t , uc_session
where uc_session.id =session_id and uc_session.status=1
