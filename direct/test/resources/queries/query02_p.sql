select straight_join distinct t. *, exists(select zzzz, b, has_now(?, ?) from table_c where table_c.id = b.id) as has_c, group_concat(t.bid separator ?) as bids from (select u.phone as user_phone, u.uid, u.login, u.fio, u.clientid, b.bid, b.bannerid, b.title, b.title_extension, b.body, b.flags, ifnull(b.domain, d.domain) as domain, b.href, b.sitelinks_set_id, if(b.banner_type in (...), b.banner_type, ?) as ad_type, b.banner_type, vc.apart, vc.build, vc.city, vc.contact_email, vc.contactperson, vc.country, vc.extra_message, vc.house, vc.im_client, vc.im_login, vc.metro, vc.name, vc.org_details_id, vc.phone, vc.street, vc.worktime, bim.image_id, bim.image_hash as image, bif.namespace, bif.mds_group_id, bif.avatars_host, bim.statusmoderate as image_statusmoderate, bimp.name as image_name, bim.priorityid as image_priorityid, bim.bannerid as image_bannerid, p.pid, p.geo, ? as yacontextcategories, c.manageruid, c.agencyuid, c.cid, c.sum, c.sum_spent, c.sum_to_pay, date_format(c.start_time, ?) as start_time, c.finish_time, c.wallet_cid, ifnull(wc.sum, ?) wallet_sum, ifnull(wc.sum_spent, ?) wallet_sum_spent, c.orderid, c.statusshow, c.statusactive, c.statusmoderate, c.statusbssynced, c.timetarget, c.timezone_id, c.name as camp_name, c.archived, wc.day_budget as wallet_day_budget, wco.day_budget_stop_time as wallet_day_budget_stop_time, co.stoptime, co.statuspostmoderate, c.day_budget, c.day_budget_show_mode, co.day_budget_daily_change_count, co.day_budget_stop_time, c.currencyconverted, ifnull(c.currency, ?) as currency, c.type, c.type as mediatype, p.adgroup_type, gmc.store_content_href, gmc.mobile_content_id, gmc.device_type_targeting, gmc.network_targeting, bmc.reflected_attrs, bmc.primary_action, aid.disclaimer_text as disclaimer, btl.tl_id as turbolanding_id, cpv.video_preview_url as content_promotion_video_preview_url, bpml.permalink, ifnull(cpromo.preview_url, cpv.video_preview_url) as content_promotion_preview_url, im.image_id as im_image_id, im.cid as im_cid, im.pid as im_pid, im.bid as im_bid, im.statusmoderate as im_statusmoderate, im.image_hash as im_image_hash, im.image_text as im_image_text, im.disclaimer_text as im_disclaimer_text, bimf.image_hash as bimf_image_hash, bimf.mds_group_id as bimf_mds_group_id, bimf.namespace as bimf_namespace, bimf.image_type as bimf_image_type, bimf.width as bimf_width, bimf.height as bimf_height, bimf.formats as bimf_formats, bimf.avatars_host as bimf_avatars_host, imp.imp_id as imp_imp_id, imp.clientid as imp_clientid, imp.name as imp_name, imp.image_hash as imp_image_hash, imp.create_time as imp_create_time, bp.banner_creative_id as bp_banner_creative_id, bp.cid as bp_cid, bp.pid as bp_pid, bp.bid as bp_bid, bp.creative_id as bp_creative_id, bp.statusmoderate as bp_statusmoderate, bp.extracted_text as bp_extracted_text, perfc.creative_id as perfc_creative_id, perfc.clientid as perfc_clientid, perfc.stock_creative_id as perfc_stock_creative_id, perfc.creative_type as perfc_creative_type, perfc.business_type as perfc_business_type, perfc.name as perfc_name, perfc.width as perfc_width, perfc.height as perfc_height, perfc.alt_text as perfc_alt_text, perfc.href as perfc_href, perfc.preview_url as perfc_preview_url, perfc.sum_geo as perfc_sum_geo, perfc.statusmoderate as perfc_statusmoderate, perfc.source_media_type as perfc_source_media_type, perfc.moderate_send_time as perfc_moderate_send_time, perfc.moderate_try_count as perfc_moderate_try_count, perfc.moderate_info as perfc_moderate_info, perfc.additional_data as perfc_additional_data, perfc.template_id as perfc_template_id, perfc.version as perfc_version, perfc.theme_id as perfc_theme_id, perfc.creative_group_id as perfc_creative_group_id, perfc.group_create_time as perfc_group_create_time, perfc.group_name as perfc_group_name, perfc.layout_id as perfc_layout_id, perfc.live_preview_url as perfc_live_preview_url, perfc.moderation_comment as perfc_moderation_comment, perfc.duration as perfc_duration, perfc.has_packshot as perfc_has_packshot, perfc.is_adaptive as perfc_is_adaptive, perfc.is_bannerstorage_predeployed as perfc_is_bannerstorage_predeployed from banners b left join vcards vc on vc.vcard_id = b.vcard_id join phrases p on b.pid = p.pid left join adgroups_dynamic gd on (gd.pid = p.pid) left join domains d on (d.domain_id = gd.main_domain_id) join campaigns c on p.cid = c.cid left join campaigns wc on wc.cid = c.wallet_cid left join camp_options co on co.cid = c.cid left join camp_options wco on wco.cid = c.wallet_cid join users u on c.uid = u.uid left join banner_images bim on bim.bid = b.bid left join banner_images_pool bimp on bimp.clientid = u.clientid and bimp.image_hash = bim.image_hash left join banner_images_formats bif on bif.image_hash = bim.image_hash left join adgroups_mobile_content gmc on p.pid = gmc.pid left join banners_mobile_content bmc on bmc.bid = b.bid left join banners_performance bp on bp.bid = b.bid left join perf_creatives perfc on perfc.creative_id = bp.creative_id left join images im on im.bid = b.bid left join banner_images_formats bimf on bimf.image_hash = im.image_hash left join banner_images_pool imp on imp.clientid = u.clientid and imp.image_hash = bimf.image_hash left join banners_additions bad on (bad.bid = b.bid and bad.additions_type = ?) left join additions_item_disclaimers aid on (aid.additions_item_id = bad.additions_item_id) left join banner_turbolandings btl on (b.bid = btl.bid) left join banners_content_promotion_video bcpv on (b.bid = bcpv.bid) left join content_promotion_video cpv on (bcpv.content_promotion_video_id = cpv.content_promotion_video_id) left join banner_permalinks bpml on bpml.bid = b.bid and bpml.permalink_assign_type = ? left join banners_content_promotion bcpromo on (bcpromo.bid = b.bid) left join content_promotion cpromo on (cpromo.id = bcpromo.content_promotion_id) left join clients cl on u.clientid = cl.clientid where ((c.sum - c.sum_spent + if(wc.cid, wc.sum - wc.sum_spent, ?) > ?)) and (b.reverse_domain like ? or ?) and ((ifnull(cl.work_currency, ?) = ? or ifnull(c.currency, ?) <> ?)) and b.bannerid > ? and b.statusmoderate = ? and b.statusshow = ? and c.orderid > ? and c.archived = ? and c.statusempty = ? and c.statusshow = ? and c.type in (...) and p.adgroup_type in (...) and p.statusmoderate = ? limit ?) t group by cid order by null