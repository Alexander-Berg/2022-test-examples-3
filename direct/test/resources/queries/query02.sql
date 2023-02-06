/* reqid "'''" in ( skdjks, ' in ( "sss", "sss" )))) */ SELECT /* reqid:2536251464353575036:direct.perl.web:searchBanners:operator=1435416461 */ straight_join /*wowow*/ distinct t.*, exists( select zzzz, b, has_now(3, 5) from table_c where table_c.id = b.id) as has_c , GROUP_CONCAT(t.bid SEPARATOR ',') AS bids FROM (select u.phone as user_phone, u.uid, u.login, u.FIO, u.ClientID

                      , b.bid, b.BannerID, b.title, b.title_extension, b.body, b.flags, IFNULL(b.domain,d.domain) AS domain, b.href
                      , b.sitelinks_set_id -- reqid:2536251464353575036:direct.perl.web:searchBanners:operator=1435416461
                      , if(b.banner_type in ('image_ad', 'mcbanner', 'cpm_banner', 'cpc_video', 'content_promotion_video', 'content_promotion'), b.banner_type, 'text') as ad_type
                      , b.banner_type

                      , vc.apart, vc.build, vc.city, vc.contact_email, vc.contactperson, vc.country, vc.extra_message, vc.house, vc.im_client, vc.im_login, vc.metro, vc.name, vc.org_details_id, vc.phone, vc.street, vc.worktime

                      , bim.image_id
                      , bim.image_hash as image, bif.namespace, bif.mds_group_id, bif.avatars_host
                      , bim.statusModerate as image_statusModerate
                      , bimp.name as image_name
                      , bim.PriorityID as image_PriorityID
                      , bim.BannerID AS image_BannerID

                      , p.pid
                      , p.geo, '' AS yacontextCategories

                      , c.ManagerUID , c.AgencyUID

                      , c.cid, c.sum, c.sum_spent, c.sum_to_pay, DATE_FORMAT(c.start_time, '%Y%m%d000000') as start_time, c.finish_time
                      , c.wallet_cid, ifnull(wc.sum, 0) wallet_sum, ifnull(wc.sum_spent, 0) wallet_sum_spent
                      , c.OrderID, c.statusShow, c.statusActive, c.statusModerate
                      , c.statusBsSynced, c.timeTarget, c.timezone_id, c.name as camp_name, c.archived
                      , wc.day_budget as wallet_day_budget
                      , wco.day_budget_stop_time as wallet_day_budget_stop_time

                      , co.stopTime, co.statusPostModerate
                      , c.day_budget, c.day_budget_show_mode, co.day_budget_daily_change_count, co.day_budget_stop_time
                      , c.currencyConverted
                      , IFNULL(c.currency, 'YND_FIXED') AS currency
                      , c.type
                      , c.type as mediaType
                      , p.adgroup_type
                      , gmc.store_content_href
                      , gmc.mobile_content_id
                      , gmc.device_type_targeting
                      , gmc.network_targeting
                      , bmc.reflected_attrs
                      , bmc.primary_action
                      , aid.disclaimer_text AS disclaimer
                      , btl.tl_id as turbolanding_id
                      , cpv.video_preview_url as content_promotion_video_preview_url
                      , bpml.permalink
                      , ifnull(cpromo.preview_url,cpv.video_preview_url) as content_promotion_preview_url
                      ,im.`image_id` AS `im_image_id`, im.`cid` AS `im_cid`, im.`pid` AS `im_pid`, im.`bid` AS `im_bid`, im.`statusModerate` AS `im_statusModerate`, im.`image_hash` AS `im_image_hash`, im.`image_text` AS `im_image_text`, im.`disclaimer_text` AS `im_disclaimer_text`,bimf.`image_hash` AS `bimf_image_hash`, bimf.`mds_group_id` AS `bimf_mds_group_id`, bimf.`namespace` AS `bimf_namespace`, bimf.`image_type` AS `bimf_image_type`, bimf.`width` AS `bimf_width`, bimf.`height` AS `bimf_height`, bimf.`formats` AS `bimf_formats`, bimf.`avatars_host` AS `bimf_avatars_host`,imp.`imp_id` AS `imp_imp_id`, imp.`ClientID` AS `imp_ClientID`, imp.`name` AS `imp_name`, imp.`image_hash` AS `imp_image_hash`, imp.`create_time` AS `imp_create_time`,bp.`banner_creative_id` AS `bp_banner_creative_id`, bp.`cid` AS `bp_cid`, bp.`pid` AS `bp_pid`, bp.`bid` AS `bp_bid`, bp.`creative_id` AS `bp_creative_id`, bp.`statusModerate` AS `bp_statusModerate`, bp.`extracted_text` AS `bp_extracted_text`,perfc.`creative_id` AS `perfc_creative_id`, perfc.`ClientID` AS `perfc_ClientID`, perfc.`stock_creative_id` AS `perfc_stock_creative_id`, perfc.`creative_type` AS `perfc_creative_type`, perfc.`business_type` AS `perfc_business_type`, perfc.`name` AS `perfc_name`, perfc.`width` AS `perfc_width`, perfc.`height` AS `perfc_height`, perfc.`alt_text` AS `perfc_alt_text`, perfc.`href` AS `perfc_href`, perfc.`preview_url` AS `perfc_preview_url`, perfc.`sum_geo` AS `perfc_sum_geo`, perfc.`statusModerate` AS `perfc_statusModerate`, perfc.`source_media_type` AS `perfc_source_media_type`, perfc.`moderate_send_time` AS `perfc_moderate_send_time`, perfc.`moderate_try_count` AS `perfc_moderate_try_count`, perfc.`moderate_info` AS `perfc_moderate_info`, perfc.`additional_data` AS `perfc_additional_data`, perfc.`template_id` AS `perfc_template_id`, perfc.`version` AS `perfc_version`, perfc.`theme_id` AS `perfc_theme_id`, perfc.`creative_group_id` AS `perfc_creative_group_id`, perfc.`group_create_time` AS `perfc_group_create_time`, perfc.`group_name` AS `perfc_group_name`, perfc.`layout_id` AS `perfc_layout_id`, perfc.`live_preview_url` AS `perfc_live_preview_url`, perfc.`moderation_comment` AS `perfc_moderation_comment`, perfc.`duration` AS `perfc_duration`, perfc.`has_packshot` AS `perfc_has_packshot`, perfc.`is_adaptive` AS `perfc_is_adaptive`, perfc.`is_bannerstorage_predeployed` AS `perfc_is_bannerstorage_predeployed` from   banners b
                      left join vcards vc on vc.vcard_id = b.vcard_id
                      join phrases p on b.pid = p.pid
                      left join adgroups_dynamic gd on (gd.pid = p.pid)
                      left join domains d on (d.domain_id = gd.main_domain_id)
                      join campaigns c on p.cid = c.cid
                      left join campaigns wc on wc.cid = c.wallet_cid
                      left join camp_options co on co.cid = c.cid
                      left join camp_options wco on wco.cid = c.wallet_cid
                      join users u on c.uid = u.uid
                      left join banner_images bim on bim.bid = b.bid
                      left join banner_images_pool bimp on bimp.ClientID = u.ClientID AND bimp.image_hash = bim.image_hash
                      left join banner_images_formats bif on bif.image_hash = bim.image_hash
                      left join adgroups_mobile_content gmc on p.pid = gmc.pid
                      left join banners_mobile_content bmc on bmc.bid = b.bid
                      left join banners_performance bp on bp.bid = b.bid
                      left join perf_creatives perfc on perfc.creative_id = bp.creative_id
                      left join images im on im.bid = b.bid
                      left join banner_images_formats  bimf on bimf.image_hash = im.image_hash
                      left join banner_images_pool imp on imp.ClientID = u.ClientID AND imp.image_hash = bimf.image_hash
                      LEFT JOIN banners_additions bad ON (bad.bid=b.bid AND bad.additions_type="disclaimer")
                      LEFT JOIN additions_item_disclaimers aid ON (aid.additions_item_id = bad.additions_item_id)
                      LEFT JOIN banner_turbolandings btl ON (b.bid = btl.bid)
                      LEFT JOIN banners_content_promotion_video bcpv ON (b.bid = bcpv.bid)
                      LEFT JOIN content_promotion_video cpv ON (bcpv.content_promotion_video_id = cpv.content_promotion_video_id)
                      LEFT JOIN banner_permalinks bpml ON bpml.bid = b.bid AND bpml.permalink_assign_type = 'manual'
                      LEFT JOIN banners_content_promotion bcpromo ON (bcpromo.bid = b.bid)
                      LEFT JOIN content_promotion cpromo ON (cpromo.id = bcpromo.content_promotion_id)
                      LEFT JOIN clients cl ON u.ClientID = cl.ClientID
     where (( c.sum - c.sum_spent + IF(wc.cid, wc.sum - wc.sum_spent, 0) > 0 )) AND (`b`.`reverse_domain` LIKE'ur.tsurt.www%' OR 0) AND ( (IFNULL(cl.work_currency,"YND_FIXED") = "YND_FIXED" OR IFNULL(c.currency, "YND_FIXED") <> "YND_FIXED") ) AND `b`.`BannerID` >'0' AND `b`.`statusModerate` ='Yes' AND `b`.`statusShow` ='Yes' AND `c`.`OrderID` >'0' AND `c`.`archived` ='No' AND `c`.`statusEmpty` ='No' AND `c`.`statusShow` ='Yes' AND `c`.`type` IN ('content_promotion','cpm_banner','cpm_deals','cpm_price','cpm_yndx_frontpage','dynamic','internal_autobudget','internal_distrib','internal_free','mcbanner','mobile_content','performance','text') AND `p`.`adgroup_type` IN ('base','dynamic','mobile_content','performance','mcbanner','cpm_banner','cpm_video','cpm_outdoor','cpm_yndx_frontpage','content_promotion_video','cpm_indoor','cpm_audio','cpm_geoproduct','cpm_geo_pin','content_promotion') AND `p`.`statusModerate` ='Yes'  limit 250000) t GROUP BY cid order by null;