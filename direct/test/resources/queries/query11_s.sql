(select bids_base.autobudgetpriority, bids_base.bid_id, bids_base.bid_type, bids_base.cid, bids_base.opts, bids_base.pid, bids_base.price, bids_base.price_context, bids_base.statusbssynced, cast(null as signed) as place from bids_base join phrases on phrases.pid = bids_base.pid where (bids_base.cid in (...) and bids_base.bid_id in (...) and bids_base.bid_id = bids_base.bid_id and bids_base.bid_type <> ? and bids_base.bid_id = bids_base.bid_id and ? = ? and ? = ? and ? = ? and not(find_in_set(?, bids_base.opts) <> ?))) union all (select ?, bids.autobudgetpriority, bids.cid, bids.id, bids.pid, bids.price, bids.price_context, bids.statusbssynced, case when bids.is_suspended = ? then ? end,