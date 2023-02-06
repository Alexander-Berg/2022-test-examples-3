module.exports = {
    host: 'http://yandex.ru',
    route: /images-xml/,
    response: `
        <?xml version="1.0" encoding="utf-8"?>
<yandexsearch version="1.0"><request><query>Бумага и бумажные изделия</query><page>0</page><sortby order="descending" priority="no">rlv</sortby><maxpassages></maxpassages><groupings><groupby attr="ii" mode="deep" groups-on-page="1" docs-in-group="1" curcateg="-1" /></groupings></request><response date="20191219T100257"><reqid>1576749777057698-793417483137123740075705-man1-3260-XML</reqid><found priority="phrase">8209</found><found priority="strict">8209</found><found priority="all">8209</found><found-human>Нашлось 8 тыс. ответов</found-human><is-local>no</is-local><results><grouping attr="ii" mode="deep" groups-on-page="1" docs-in-group="1" curcateg="-1"><found priority="phrase">7788</found><found priority="strict">7788</found><found priority="all">7788</found><found-docs priority="phrase">7788</found-docs><found-docs priority="strict">7788</found-docs><found-docs priority="all">7788</found-docs><found-docs-human>нашёл 8 тыс. ответов</found-docs-human><page first="1" last="1">0</page><group><categ attr="ii" name="370731524193751924" /><doccount>1</doccount><relevance>106117440</relevance><doc id="ZD9B6B1FAEC93E0BB"><relevance>106117440</relevance><url>https://s-trade54.ru/sites/default/files/img-news/123_0.jpg</url><domain>s-trade54.ru</domain><title>Производство туалетной бумаги как бизнес: расчеты</title><modtime>20170908T101810</modtime><size>0</size><charset>utf-8</charset><passages><passage>Такой бизнес, как производство и продажа туалетной <hlword>бумаги</hlword>, очень выгоден, поскольку не требует много затрат</passage></passages><properties><NNFeatures>GsgBDQcD_gAA_PkH_AEB9_0A_AIFBv0CBP8A-vj__P7_-QACBAT9__4B_AIAAH8EAQD_CP0A-gECBAb5_AQCB_oCAf8A-PkBAwH-Bv_9AP4A_P75A_8DBv4D_vsCAPgE9vf5BvcDBAUXAgH-AAD7AP4DCAdCA_wADPr7AAYA_gEJ-f8AAgr2Aev_9vwAAwMP-QLy-vQG7PwF_Rv77Qn7AvkDBP_69vj9BQAN8wcA_f8VAP78AQAAHQL9-wcA__7y_UoABf399QoA9QAqyAHs4yIkysghDArn9tUM9vwHZv3VBi_jQ9EKyBr4AfzoCCYwFzz0_wX78-wc6NcX1wsQ3cvNwast1kE8QjT98cFR2Dz0_P0IER1I6yWBziP1PfMfGNf68_3UD-TcXloRHgioV8DbWPfp6O3YHCT-txIt6cf45x0kCuMZKtAkEeIH1OkdFODsHRwa1AFL6EgG5BUg_wgBPTL97jMUF9Y5WAID0POh8SjR5dv6zwAW7tLD88krFd7yKNP20R7s-yLszcTyDh8MC-LnwjKWAaDy3A0ZGCL-DiZFO6EQU1oFjBSBLxtyFRhM540VH9H74SApulaGMRTXeKFDSlNY5UMpeKR4WyNVt0dnDdS3UGcNx3Grbi43TDFvGJqT1HEWFNzYeC1KwEp_D4ktJ4cQFqrsiis6EA2NDU_SHp0vgvFtqRGryWizX2Dk57oZ1XWEwQ-Dv37oDTZV5-hBOwzP7TB4hvD1Dw,,</NNFeatures><_CompressedAllFactors>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</_CompressedAllFactors><_ImagesAbsRel>0.6657919579</_ImagesAbsRel><_ImagesDups>i\t52371c9c98121eb47eeb1486b9a55a27\tc\t16844685775784814418\tw\t1321\th\t878\ttw\t480\tth\t319\tu\thttps://image.jimcdn.com/app/cms/image/transf/none/path/s0d5bcf3efdd9165a/image/i1db8f643f1192fbe/version/1419687199/image.jpg\tuh\thttps://salfetkis.jimdo.com/\ts\t163492\tt\tjpg\tbq\t246\ttxt_img_sim\t0.6144\tbi\t869b8c1a7a4868b78743247e0ac1673e-l\tbtw\t1321\tbth\t878
i\t30e69e04d2edcce9a74430344d1bb8ef\tc\t8775944706171476042\tw\t800\th\t600\ttw\t427\tth\t320\tu\thttps://oplata.me/wp-content/uploads/2-28.jpg\tuh\thttps://oplata.me/proizvodstvo/tualetnoj-bumagi-kak-biznes.html\ts\t56585\tt\tjpg\tbq\t239\ttxt_img_sim\t0.6085</_ImagesDups><_ImagesJson>{&quot;dc&quot;:&quot;white&quot;,&quot;Passages&quot;:[{&quot;qhits&quot;:&quot;D&quot;,&quot;di&quot;:1,&quot;lang&quot;:1,&quot;title&quot;:&quot;Производство туалетной бумаги как бизнес: расчеты&quot;,&quot;text&quot;:&quot;Такой бизнес, как производство и продажа туалетной \u0007[бумаги\u0007], очень выгоден, поскольку не требует много затрат&quot;}],&quot;Preview&quot;:[{&quot;di&quot;:1}]}</_ImagesJson><_Markers>SnipDebug=regphrase=;uil=ru;tld=ru;report=xml;exps=imgbuild;rsdups=0;pvdups=2;t=2;screenw=1366;fsframeh=768;frameh=563;framew=926;fsframew=1300;screenh=768;</_Markers><_Markers>documentid=+rG22bvgk+wAAAAAAAAAAA==</_Markers><_Markers>SnipImageRanker=visquality_boost_ranker_viewportsize_1d5</_Markers><_Markers>SnipTextRanker=page_relevance_ranker</_Markers><_Markers>SnipTextFastRanker=images_stabilizer#weighted_factors_ranker</_Markers><_Markers>RandomLog=eyJUaXRsZSI6IiIsIkFsbEZhY3RvcnNDcmF0ZSI6IkFTTUFBQUJoYkd4Yk1EczNNREVwSUdsdFlXZGxjMTl3Y205a2RXTjBhVzl1V3pBN056QXhLZHNGQUFEK3B5d2FtSWFDZ3NRQUFBQUFBSUN3cXFyWXFxb2F6Y3djT2NsN1hPZWNFUURBSXdDQVI2aWZqZ0FBSHNWT0xETGZjcVE3MkFnQTRCRUF3TE9hbWpvcUd2OXNabWFHQUFCQVdGVlZ3VTdzTkR1eEVcL2tTbW5mTnhjVUZwOWFEUit3Y3U3bTVlbXFiejF0aUZ0YVpLd0RBZUJlOXU3dWdtcHE5UTh3Q291SGdZTVJVOVY5bHFINTR0OTZwMWhtcGo4R1wvUDY0Q1JocUNJSURCdVwvdm9SM0NOcU1FVThVVGZBQUFBMGYwODEwaElDQmtNREhSXC9Bd0FBVUQ4XC9ZeTlWcXlibDJ4UmZlbThHQWdJQ0lJT0JnVVwvc0luM2tcL3Y5SFwvXC80dWtxYmZoYTVHK2x4VlZZMXJacVpPakxTaGk4SUJBQUNnN1NXMG9BKzhEZWo2MTJxb0txa01wS1RWVUZWU0dVaEpBZUxcL3BES1FFbGRWVlJVZitGNnJvYXFrTXBDU1ZrTlZBY1RcL000TFhRb0Q5ZjJ5QW13MG5XekdrRk1KUTAza1VIWTFhRFZVbGxZR1UwQWpMY2ZYeEh5RCtueEc4RnJqUEpodk15eXRTK0twb29EZXVFR1NtUEJ3T04rQXBTdXptWmtRXC9QOWk4OGpuOUNpXC9FeDNFRXhJUUVQajBFYW1Ua2x3clhJM2liQmdpam9xS2hvQ0NvSzhYUXpNeFFlTktMS2NKSTUzUXdpN29rd015YUVUSlhIXC9sS1l4OXJPTUVwWnpmT1ozVjBwdm9hdVljb2VtaCs1a2V1WWtrOWExVUJWSFNzS29DS2psVUZVTkd4cWdBcU9qSVFFQkFLQkFRaTZyYlBLbkJvR2JaZ0s4TVdiR1VvS0tnTUJRVkZ5TmM5eUFoTTJPaTdGbWx6SjNDS0Rva1d2S2NWYmJzQUV2bVJzY0dhZDIwZkFKaDNiUjhBMElTSTE2b0NxT1NjQWNcL05lVmJnajhNUllqVkMzMDBBQUFCRVdYaDNBZjM4aEhKeTh2eFwvNFlhQmlFNlNcL29KR1wvdjJ0QUFEQUVnQnc5RXVGNjVFU0FJQmVCZ0lDYWlRa0pHTmhZZkZKU1FrTUMwc1FEWTNnVWppMVVzeVVYMWdZRVFBQUlQOW9oekxHR0ZHd0FkZGFhOUdVd1F0akF5U1hTa1ZnWUVHQ2dRVkpMcFdLR0Z3NHNoa0lDQWlBa0lxS1lGQlFVTEJwbzlFUWJ4eWp1Tmc3ZGNLcXFqTE8wUlhJUWhiaU9JN1VSUmVIQUFDQWRORkYxRVVYQndBQTBDZ2lJa05WVmVPYW1Za0RBQ0Q1R0p6QkVFQ0NkTkhGdG0yYjkrV3gwRHkyNmlKb3VDNkNob3MwbkFRQUFBQUFBQUFneDNFQkFBQUFBQURVUUVDQVdEZzRDS1NqNCtLM3QyY3RMYTNDUXNBcUxBUnNNeEFRRUFBekVCQVFnQ3hxR1lmWm9oM1dRV05uVzh5dGFPQVdBZUxoMGRMdG1wd2VBNmpERkRIOGNoWG93aFdXVldcL1krTXZSOUV0ejdQc2ZoaGx4WWNGRkVjalwvdzhLbkFOdG1PZXoybURCb295TFwvXC8wZEY0NCtlOGg5WjFEXC9LQzNcL1VcL3Y5SVlPdkR4NitLXC9QOGZlWllcLzRpSlwvaEpiXC9TS1wvK1VYMzRvXC9mXC9SeVpCamhZNUhyR2tQN0x0ZjRSWlwvU2plXC94SDhcL3lPazMwY21cL284UTJ4XC90SHozQ1wvMzlVS2ZZSVRic1JiM2s2enBCSnZnd0VCRGkwcUtnc0p5ZVQ0ZURnTWhBUUFBUUNBbnhyYTRSNHg1QUdiTkxDMFprcFlzZ084SHBQOUJLckltM3U2SHZ2SFdVMEdRUnVhTkdNTVVaMDJXU0lCdldSb2lZaitCMVRwWlFTOVwvXC9cL3oxWldGclN2cnhaTnN6bUhcL2Z6OElJRm1PM0JzN09obzlOSFI2TmNFdFFUczNkM0ZiMjlcL0crRGdwcU1cL2dudndyS3lRdWtnZk9IV2VEQVlHZG8yRWhQQ09ORDBYb1hjXC9rb0I2WkNQZ25yT3lzdUpDd1NEbmJVY0tiNHVvYVVnNjU1eWpHaFNwS2FXVVwvQWtReGZadlJ0ZGFhOUdPZ1lzZVQ2RUFBS3pydEtwMnFhSTZ0TzVoQkVwQkRGTDZoZGdDa0NCakxDd01QSFwvVVJBZ2hpXC83OUhlXC9rNUg5c3NCQTN3NUxCQWJnTHE2cnFyS1NrZEZZTEdCa2ZRaTJ6eFNRXC9sbVNXaUs3YmtXd3hJeU10MDR3eFJtcUtkOE55NWd6ZXY1czI3cUthVWtyeFh4SUlCRmFEd2FCZ01MTTErY1dTUDZPaVwvQmtWd1RzNUdmMzdPeHdMQzN4MmRsNEdBd05yTURBUTYrbnBOUjRlRGlRaUlvQjhmR05aV1RIMjljVTVOMWR3YmUxMHZIVG43WHd2S0RFSWlzREs0NWh1QWdSV0RsUzRIamcwTk9qUTBPaTBuQm9YYzI1Y3BYaUlcL29zTjFzR2hEQVVGSFVBK3ZnaUlWeklRRU1BNE5qWTRGeGZIenMydG8ydjg1N1wvZXlQXC9cL3I4SEF3Qm9QRDVmRnhFUkJJeVBcL1grUGg0ZWJ3NXFSS1wvYklJcUs0TWcwYkpqMk5lM3ZcL3Y1dlwvXC9QekVBQUFBQUFBQUFBQT09In0=</_Markers><_MetaSearcherHostname>man1-1521.search.yandex.net:9200</_MetaSearcherHostname><_MetaSearcherHostname>man1-1854.search.yandex.net:9402</_MetaSearcherHostname><_MetaSearcherHostname>man1-3260.search.yandex.net:9080</_MetaSearcherHostname><_MimeType>2 0&amp;d=489&amp;sh=-1&amp;sg=</_MimeType><_SearcherHostname>man2-5585.search.yandex.net:9300</_SearcherHostname><_Shard>imgsidx-300-20191215-135427</_Shard><documentid>+rG22bvgk+wAAAAAAAAAAA==</documentid><imagetags>Ci8KDdCx0YPQvNCw0LPQsCsSDNCx0YPQvNCw0LPQsB30ypZDLQAA-EEyBggAEAAYAApWCiHRgtGD0LDQu9C10YLQvdGL0LkrINCx0YPQvNCw0LPQsCsSH9GC0YPQsNC70LXRgtC90LDRjyDQsdGD0LzQsNCz0LAd_C_sQi0AABBBMgYIABAAGAAKWQog0LHRg9C80LDQttC9INC_0L7Qu9C-0YLQtdC90YbQtSsSI9Cx0YPQvNCw0LbQvdGL0LUg0L_QvtC70L7RgtC10L3RhtCwHYPGjEItAACgQDIGCAAQABgACjkKEdC80LDRgtC10YDQuNCw0LsrEhLQvNCw0YLQtdGA0LjQsNC70Ysd_9N-Qi0AAABBMgYIABAAGAAKOwoT0L_RgNC-0LTRg9C60YbQuNGPKxIS0L_RgNC-0LTRg9C60YbQuNGPHep0ZEItAADAQDIGCAAQABgACl8KJNGA0LDRgdGF0L7QtNC90YvQuSDQvNCw0YLQtdGA0LjQsNC7KxIl0YDQsNGB0YXQvtC00L3Ri9C1INC80LDRgtC10YDQuNCw0LvRix3b5jxCLQAAgEAyBggAEAAYAAorCgtzdGF0aW9uYXJ5KxIKc3RhdGlvbmFyeR3q6UFBLQAAgD8yBggAEAAYAAojCgdwYXBpZXIrEgZwYXBpZXIdDOksQS0AAIA_MgYIABAAGAAKIQoGcGFwZWwrEgVwYXBlbB2hfClBLQAAgD8yBggAEAAYAAonCglzdXBwbGllcisSCHN1cHBsaWVyHdNUB0EtAACAPzIGCAAQABgA</imagetags></properties><image-properties><id>30e69e04d2edcce9a74430344d1bb8ef</id><gid>+rG22bvgk+w</gid><shard>0</shard><thumbnail-width>150</thumbnail-width><thumbnail-height>112</thumbnail-height><thumbnail-width-original>427</thumbnail-width-original><thumbnail-height-original>320</thumbnail-height-original><thumbnail-link>http://im0-tub-ru.yandex.net/i?id=30e69e04d2edcce9a74430344d1bb8ef</thumbnail-link><original-width>800</original-width><original-height>600</original-height><html-link>oplata.me/proizvodstvo/tualetnoj-bumagi-kak-biznes.html</html-link><image-link>oplata.me/wp-content/uploads/2-28.jpg</image-link><file-size>56585</file-size><mime-type>jpg</mime-type><dominated_color>white</dominated_color></image-properties><mime-type>text/html</mime-type><highlight-cookie>0&amp;d=489&amp;sh=-1&amp;sg=</highlight-cookie><image-duplicates></image-duplicates><image-duplicates-preview><image-properties><id>30e69e04d2edcce9a74430344d1bb8ef</id><gid></gid><shard>0</shard><thumbnail-width>150</thumbnail-width><thumbnail-height>112</thumbnail-height><thumbnail-width-original>427</thumbnail-width-original><thumbnail-height-original>320</thumbnail-height-original><thumbnail-link>http://im0-tub-ru.yandex.net/i?id=30e69e04d2edcce9a74430344d1bb8ef</thumbnail-link><original-width>800</original-width><original-height>600</original-height><html-link>oplata.me/proizvodstvo/tualetnoj-bumagi-kak-biznes.html</html-link><image-link>oplata.me/wp-content/uploads/2-28.jpg</image-link><file-size>56585</file-size><mime-type>jpg</mime-type></image-properties></image-duplicates-preview><image-duplicates-resized></image-duplicates-resized></doc></group></grouping></results></response></yandexsearch>
        `,
};