select count(1) from supercontroller_cache
union all
select count(1) from supercontroller_offer_queue
union all
select count(1) from supercontroller_cache where matched_id > 0
union all
select count(1) from supercontroller_cache where mapped_id > 0
union all
select count(1) from supercontroller_cache where category_id > 0