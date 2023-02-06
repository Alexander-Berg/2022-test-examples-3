SELECT /* reqid:2522342514668344484:direct.script:ppcResendDomainsBS */ straight_join.a, /*wowow*/ distinct.b, b.cid, b.bid, v.phone, t.bannerName
                  FROM banners  b
                       JOIN vcards v ON v.vcard_id = b.vcard_id and v.id = b.id  
                       inner join (select distinct id, distinct_bannerName, straight_join_t.yql, a.format from texts t) t on t.id = b.text_id
                       inner join phrases s using(id) -- проверка using
                 WHERE b.reverse_domain IS NULL
                       AND v.phone IS NOT NULL
                       AND v.phone != ''
                       and input = 'test' -- тестовый вход
                       and v.id = 1
                 GROUP BY v.phone
                 ORDER BY null;

