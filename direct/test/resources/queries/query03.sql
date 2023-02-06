SELECT /* reqid:2647883382999770505:direct.jsonrpc:Moderation_process_mod_result */ /* sendppc */straight_join /*wowow*/ distinct 
                                                        c.statusModerate -- надо получить статус модерации
                                                        , co.statusPostModerate
                                                        , count(*) cnt
                                                        , sum( IF( b.statusModerate = 'New' or p.statusModerate = 'New', 1, 0 ) ) cnt_new
                                                        , sum( IF( b.statusModerate in ('Yes', 'No') or p.statusModerate in ('Yes', 'No') or b.phoneflag in ('Yes', 'No'), 1, 0 ) ) cnt_mod
                                                        , sum( IF( b.statusModerate = 'Yes'
                                                                   and ( IFNULL(b.href, '') != ''
                                                                      or b.phoneflag = 'Yes' -- or may be 'No'
                                                                      or t.statusModerate = 'Yes'
                                                                      or b.banner_type IN ('dynamic', 'mobile_content')
                                                                      or (b.banner_type IN ('image_ad', 'cpc_video') and c.type = 'mobile_content')
                                                                      or bp.permalink > 0 )
                                                                   and p.statusModerate = 'Yes'
                                                                   and COALESCE(i.statusModerate, b_perf.statusModerate, 'Yes') = 'Yes', 1, 0
                                                                 )
                                                             ) cnt_accept
                                                        , sum( IF( b.statusModerate = 'Yes' and ( IFNULL(b.href, '') = '' and b.phoneflag = 'Wait' ) and p.statusModerate = 'Yes', 1, 0 ) ) cnt_wait
                                                        , sum( IF( b.statusModerate = 'No'
                                                                   or p.statusModerate = 'No'
                                                                   or COALESCE(i.statusModerate, b_perf.statusModerate, 'Yes') = 'No'
                                                                   or (IFNULL(b.href, '') = '' and b.phoneflag = 'No')
                                                                   or (IFNULL(b.href, '') = '' and t.statusModerate = 'No'), 1, 0
                                                                 )
                                                             ) cnt_decline
                                                        , sum( IF((b.statusModerate = 'Yes' or b.statusPostModerate = 'Yes')
                                                                   and COALESCE(i.statusModerate, b_perf.statusModerate, 'Yes') = 'Yes'
                                                                   and (p.statusModerate = 'Yes' or p.statusPostModerate = 'Yes')
                                                                   and (b.href is not null or b.phoneflag = 'Yes'
                                                                       or t.statusModerate = 'Yes'
                                                                       or b.banner_type IN ('dynamic', 'mobile_content') ), 1, 0
                                                                 )
                                                             ) cnt_preliminarily
                                                        , sum( IF(p.statusModerate = 'Yes', 1, 0) ) AS group_cnt_accepted
                                                        , c.AgencyUID, c.ManagerUID, c.OrderID, c.sum, c.sum_to_pay, c.uid
                                                        , c.wallet_cid
                                                        , c.type, c.cid
                                                    FROM campaigns c
                                                        left join camp_options co using(cid)
                                                        JOIN phrases p ON p.cid = c.cid
                                                        JOIN banners b ON p.pid = b.pid
                                                        LEFT JOIN images i ON b.bid = i.bid
                                                        LEFT JOIN banners_performance b_perf on (b.banner_type = 'image_ad' or b.banner_type = 'cpc_video') and b_perf.bid = b.bid
                                                        LEFT JOIN banner_turbolandings t ON (b.cid = t.cid AND b.bid = t.bid)
                                                        LEFT JOIN banner_permalinks bp ON bp.bid = b.bid AND bp.permalink_assign_type = 'manual'
                                                    WHERE
                                                        c.cid = '51316653'
                                                        AND c.ClientID % 100 >= 0  GROUP BY
                                                        c.statusModerate;