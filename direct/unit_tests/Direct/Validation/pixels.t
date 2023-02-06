use Direct::Modern;

use Test::More;
use Yandex::Test::ValidationResult;
use Direct::Model::AdGroupCpmBanner;
use Direct::Model::CampaignCpmBanner;
use Direct::Model::CampaignCpmDeals;
use Direct::Model::Pixel;

use Settings;

use constant AUDIT_GEMIUS_URL => 'https://gdeby.hit.gemius.pl/_%aw_random%/redot.gif?id=bVdLcgtpfJcK8F5yqvK7naP8zUsi2I7Q5bo9ydeoYoP.p7/fastid=igjvgufcuuhxoigmzdimcqemvblf/stparam=zaolqinkmf';

BEGIN {
    use_ok('Direct::Validation::Pixels', qw/validate_pixels/);
}


sub mk_pixels {
    my ($url, $kind) = @_;
    return Direct::Model::Pixel->new(
        url => $url,
        defined $kind ? (kind => $kind) : (),
    );
}

local *vr = sub { my ($pixels_data, $permitted_providers, $adgroup) = @_; return validate_pixels([map { mk_pixels(@$_) } @$pixels_data], $permitted_providers, $adgroup) };

my $cpm_banner_adgroup = Direct::Model::AdGroupCpmBanner->new(campaign => Direct::Model::CampaignCpmBanner->new());
my $cpm_deals_adgroup_yandex_with_private = Direct::Model::AdGroupCpmBanner->new(
    campaign => Direct::Model::CampaignCpmDeals->new(is_yandex_page => 1),
    has_private_criterion => 1
);
my $cpm_deals_adgroup_not_yandex_with_private = Direct::Model::AdGroupCpmBanner->new(
    campaign => Direct::Model::CampaignCpmDeals->new(is_yandex_page => 0),
    has_private_criterion => 1
);
my $cpm_deals_adgroup_yandex = Direct::Model::AdGroupCpmBanner->new(
    campaign => Direct::Model::CampaignCpmDeals->new(is_yandex_page => 1),
    has_private_criterion => 0
);
my $cpm_deals_adgroup_not_yandex = Direct::Model::AdGroupCpmBanner->new(
    campaign => Direct::Model::CampaignCpmDeals->new(is_yandex_page => 0),
    has_private_criterion => 0
);
my $cpm_deals_adgroup_no_page = Direct::Model::AdGroupCpmBanner->new(
    campaign => Direct::Model::CampaignCpmDeals->new(is_yandex_page => undef),
    has_private_criterion => 0
);
my $cpm_deals_adgroup_no_page_with_private = Direct::Model::AdGroupCpmBanner->new(
    campaign => Direct::Model::CampaignCpmDeals->new(is_yandex_page => undef),
    has_private_criterion => 1
);

subtest 'good_pixels' => sub {
    ok_validation_result(vr([['https://mc.yandex.ru/pixel/2555327861230035827?rnd=%random%', 'audience']], undef, $cpm_banner_adgroup));
    ok_validation_result(vr([['https://mc.yandex.ru/pixel/2555327861230035827?rnd=%aw_random%', 'audience']], undef, $cpm_banner_adgroup));
    ok_validation_result(vr([['https://mc.yandex.ru/pixel/2555327861230035827?rnd=%25aw_random%25', 'audience']], undef, $cpm_banner_adgroup));
    ok_validation_result(vr([['https://ads.adfox.ru/254364/getCode?p1=bxoar&p2=v&pfc=bnkqk&pfb=failv&pr=%25aw_RANDOM%25&ptrc=b', 'audit']], undef, $cpm_banner_adgroup));
    ok_validation_result(vr([['https://ads.adfox.ru/254364/getCode?p1=bxoar&p2=v&pfc=bnkqk&pfb=failv&pr=%25RaNdOM%25&ptrc=b', 'audit']], undef, $cpm_banner_adgroup));
    ok_validation_result(vr([['https://ads.adfox.ru/254364/getCode?p1=bxoar&p2=v&pfc=bnkqk&pfb=failv&pr=%RaNdOM%&ptrc=b', 'audit']], undef, $cpm_banner_adgroup));
    ok_validation_result(vr([['https://mc.yandex.ru/pixel/2555327861230035827?rnd=%random%', 'audience'], ['https://ads.adfox.ru/254364/getCode?p1=bxoar&p2=v&pfc=bnkqk&pfb=failv&pr=%RaNdOM%&ptrc=b', 'audit']], undef, $cpm_banner_adgroup));

    foreach my $adgroup ($cpm_deals_adgroup_yandex, $cpm_deals_adgroup_yandex_with_private) {
        ok_validation_result(vr([['https://mc.yandex.ru/pixel/2555327861230035827?rnd=%random%', 'audience']], undef, $adgroup));
    }

    foreach my $adgroup ($cpm_deals_adgroup_yandex, $cpm_deals_adgroup_not_yandex, $cpm_deals_adgroup_yandex_with_private, $cpm_deals_adgroup_not_yandex_with_private, $cpm_deals_adgroup_no_page, $cpm_deals_adgroup_no_page_with_private) {
        ok_validation_result(vr([['https://ads.adfox.ru/254364/getCode?p1=bxoar&p2=v&pfc=bnkqk&pfb=failv&pr=%25aw_RANDOM%25&ptrc=b', 'audit']], undef, $adgroup));
    }
};

subtest 'wrong_pixels' => sub {
    cmp_validation_result(vr([['http://mc.yandex.ru/pixel/2555327861230035827?rnd=%aw_random%', 'audience']], undef, $cpm_banner_adgroup), { audience => vr_errors('InvalidFormat') } );
    cmp_validation_result(vr([['https://mc.yandex.ru/pixel/2555327861230035827?rnd=%random%', 'audit']], undef, $cpm_banner_adgroup), { audit => vr_errors('InvalidFormat') });
    cmp_validation_result(vr([['https://mc.yandex.ru/pixel/2555327861230035827?rnd=%rand%', 'audience']], undef, $cpm_banner_adgroup), { audience => vr_errors('InvalidFormat') } );
    cmp_validation_result(vr([['https://mcc.yandex.ru/pixel/2555327861230035827?rnd=%random%', 'audience']], undef, $cpm_banner_adgroup), { audience => vr_errors('InvalidFormat') } );
    cmp_validation_result(vr([['https://ads.adfox.ru/254364/getCode?p1=bxoar&p2=v&pfc=bnkqk&pfb=failv&pr=%RaNd%&ptrc=b', 'audit']], undef, $cpm_banner_adgroup), { audit => vr_errors('InvalidFormat') });
    cmp_validation_result(vr([['https://www.tns-counter.ru/V13a****bbdo_ad/ru/UTF-8/tmsec=bbdo_cid1021927-posid1323236/%aw_random%', 'audience']], undef, $cpm_banner_adgroup), { audience => vr_errors('InvalidFormat') });
    cmp_validation_result(vr([['https://1tns-counter.ru/V13a****bbdo_ad/ru/UTF-8/tmsec=bbdo_cid1021927-posid1323236/%aw_random%', 'audience']], undef, $cpm_banner_adgroup), { audience => vr_errors('InvalidFormat') });
    cmp_validation_result(vr([['https://.tns-counter.ru/V13a****bbdo_ad/ru/UTF-8/tmsec=bbdo_cid1021927-posid1323236/%aw_random%', 'audience']], undef, $cpm_banner_adgroup), { audience => vr_errors('InvalidFormat') });
    cmp_validation_result(vr([['https://ads.adfox.ru/254364/getCode?p1=bxoar&p2=v&pfc=bnkqk&pfb=failv&pr=%RaNdOM%&ptrc=b', 'audit'], ['https://www.tns-counter.ru/V13a****bbdo_ad/ru/UTF-8/tmsec=bbdo_cid1021927-posid1323236/%aw_random%', 'audit']], undef, $cpm_banner_adgroup), vr_errors('InconsistentState'));
    cmp_validation_result(vr([['https://mc.yandex.ru/pixel/2555327861230035827?rnd=%random%', 'audience'], ['https://mc.yandex.ru/pixel/2555327861230035827?rnd=%random%', 'audience']], undef, $cpm_banner_adgroup), vr_errors('InconsistentState'));
    cmp_validation_result(vr([['https://mc.yandex.ru/pixel/2555327861230035827?rnd=%rand%', 'audience'], ['https://ads.adfox.ru/254364/getCode?p1=bxoar&p2=v&pfc=bnkqk&pfb=failv&pr=%RaNd%&ptrc=b', 'audit']], undef, $cpm_banner_adgroup), { audience => vr_errors('InvalidFormat'), audit => vr_errors('InvalidFormat') });
    cmp_validation_result(vr([['https://ad.adriver.ru/cgi-bin/rle.cgi?sid=1&ad=111&bt=21&pid=222&bid=431&bn=431&rnd=%random%', 'audit'], ['https://ads.adfox.ru/254364/getCode?p1=bxoar&p2=v&pfc=bnkqk&pfb=failv&pr=%RaNdOM%&ptrc=b', 'audit']], { none => ['adriver'] }, $cpm_banner_adgroup), vr_errors('InconsistentState'));
    cmp_validation_result(vr([['https://ad.adriver.ru/cgi-bin/rle.cgi?sid=1&ad=111&bt=21&pid=222&bid=431&bn=431&rnd=%random%', 'audit'], ['https://wcm.solution.weborama.fr/fcgi-bin/dispatch.fcgi?a.A=im&a.si=7&a.te=3474&a.he=1&a.wi=1&a.hr=p&a.ra=%aw_random%', 'audit']], { none => [qw/adriver weborama/] }, $cpm_banner_adgroup), vr_errors('InconsistentState'));
};

subtest 'non-permitted pixels' => sub {
    foreach my $adgroup ($cpm_banner_adgroup, $cpm_deals_adgroup_not_yandex_with_private, $cpm_deals_adgroup_yandex, $cpm_deals_adgroup_yandex_with_private, $cpm_deals_adgroup_no_page, $cpm_deals_adgroup_no_page_with_private) {
        cmp_validation_result(vr([['https://ad.adriver.ru/cgi-bin/rle.cgi?sid=1&ad=111&bt=21&pid=222&bid=431&bn=431&rnd=%random%', 'audit']], undef, $adgroup), { audit => vr_errors('InvalidFormat') });
        cmp_validation_result(vr([['https://bs.serving-sys.com/serving/adserver.bs?cn=display&c=32&pli=1074190221&ord=%random%', 'audit']], { none => ['adriver'] }, $adgroup), { audit => vr_errors('InvalidFormat') });
        cmp_validation_result(vr([['https://wcm.solution.weborama.fr/fcgi-bin/dispatch.fcgi?a.a=im&a.si=7&a.te=3474&a.he=1&a.wi=1&a.hr=p&a.ra=%aw_random%', 'audit']], { none => [qw/adriver sizmek/] }, $adgroup), { audit => vr_errors('InvalidFormat') });
        cmp_validation_result(vr([['https://ad.doubleclick.net/ddm/ad/n719887.279382dbmyandexru4863976/b20224086.203459985;sz=728x90;ord=%random%', 'audit']], undef, $adgroup), { audit => vr_errors('InvalidFormat') });
        cmp_validation_result(vr([[AUDIT_GEMIUS_URL, 'audit']], undef, $adgroup), { audit => vr_errors('InvalidFormat') });
        cmp_validation_result(vr([['https://www.tns-counter.ru/V13a****bbdo_ad/ru/UTF-8/tmsec=bbdo_cid1021927-posid1323236/%25aw_random%25', 'audit']], undef, $adgroup), { audit => vr_errors('InvalidFormat') });
        cmp_validation_result(vr([['https://www.tns-counter.ru/V13a****bbdo_ad/ru/UTF-8/tmsec=bbdo_cid1021927-posid1323236/%aw_random%', 'audit']], undef, $adgroup), { audit => vr_errors('InvalidFormat') });
        cmp_validation_result(vr([['https://tns-counter.ru/V13a****bbdo_ad/ru/UTF-8/tmsec=bbdo_cid1021927-posid1323236/%aw_random%', 'audit']], undef, $adgroup), { audit => vr_errors('InvalidFormat') });
        cmp_validation_result(vr([['https://'.$_.'.tns-counter.ru/V13a****bbdo_ad/ru/UTF-8/tmsec=bbdo_cid1021927-posid1323236/%aw_random%', 'audit']], undef, $adgroup), { audit => vr_errors('InvalidFormat') })
            foreach qw/ar kz maxus mp rw sup tv www yandex2 yandex/;
    }

    foreach my $adgroup ($cpm_deals_adgroup_not_yandex_with_private, $cpm_deals_adgroup_not_yandex, $cpm_deals_adgroup_no_page, $cpm_deals_adgroup_no_page_with_private) {
        cmp_validation_result(vr([['https://mc.yandex.ru/pixel/2555327861230035827?rnd=%random%', 'audience']], undef, $adgroup), { audience => vr_errors('BadUsage') });
    }

    foreach my $adgroup ($cpm_deals_adgroup_no_page, $cpm_deals_adgroup_no_page_with_private) {
        cmp_validation_result(vr([['https://ad.adriver.ru/cgi-bin/rle.cgi?sid=1&ad=111&bt=21&pid=222&bid=431&bn=431&rnd=%random%', 'audit']], { none => ['adriver'], private => ['adriver'], public => ['adriver'], yandex => ['adriver'] }, $adgroup), { audit => vr_errors('InvalidFormat') });

        cmp_validation_result(vr([['https://bs.serving-sys.com/serving/adserver.bs?cn=display&c=32&pli=1074190221&ord=%random%', 'audit']], { none => ['sizmek'], private => ['sizmek'], public => ['sizmek'], yandex => ['sizmek'] }, $adgroup), { audit => vr_errors('InvalidFormat') });

        cmp_validation_result(vr([['https://ad.doubleclick.net/ddm/ad/n719887.279382dbmyandexru4863976/b20224086.203459985;sz=728x90;ord=%random%', 'audit']], { none => ['dcm'], private => ['dcm'], public => ['dcm'], yandex => ['dcm'] }, $adgroup), { audit => vr_errors('InvalidFormat') });

        cmp_validation_result(vr([[AUDIT_GEMIUS_URL, 'audit']], { none => ['gemius'], private => ['gemius'], public => ['gemius'], yandex => ['gemius'] }, $adgroup), { audit => vr_errors('InvalidFormat') });

        cmp_validation_result(vr([['https://wcm.solution.weborama.fr/fcgi-bin/dispatch.fcgi?a.a=im&a.si=7&a.te=3474&a.he=1&a.wi=1&a.hr=p&a.ra=%aw_random%', 'audit']], { none => ['weborama'], private => ['weborama'], public => ['weborama'], yandex => ['weborama'] }, $adgroup), { audit => vr_errors('InvalidFormat') });
        cmp_validation_result(vr([['https://mc.yandex.ru/pixel/2555327861230035827?rnd=%random%', 'audience']], { none => ['yndx_audience'], yandex => ['yndx_audience'], public => ['yndx_audience'], private => ['yndx_audience'] }, $adgroup), { audience => vr_errors('BadUsage') });
    }
};

subtest 'permitted pixels' => sub {
    ok_validation_result(vr([['https://ad.adriver.ru/cgi-bin/rle.cgi?sid=1&ad=111&bt=21&pid=222&bid=431&bn=431&rnd=%random%', 'audit']], { none => ['adriver'] }, $cpm_banner_adgroup));
    ok_validation_result(vr([['https://bs.serving-sys.com/serving/adserver.bs?cn=display&c=32&pli=1074190221&ord=%random%', 'audit']], { none => ['sizmek'] }, $cpm_banner_adgroup));
    ok_validation_result(vr([['https://wcm.solution.weborama.fr/fcgi-bin/dispatch.fcgi?a.a=im&a.si=7&a.te=3474&a.he=1&a.wi=1&a.hr=p&a.ra=%aw_random%', 'audit']], { none => ['weborama'] }, $cpm_banner_adgroup));
    ok_validation_result(vr([['https://ad.doubleclick.net/ddm/ad/n719887.279382dbmyandexru4863976/b20224086.203459985;sz=728x90;ord=%random%', 'audit']], { none => ['dcm'] }, $cpm_banner_adgroup));
    ok_validation_result(vr([[AUDIT_GEMIUS_URL, 'audit']], { none => ['gemius'] }, $cpm_banner_adgroup));
    ok_validation_result(vr([['https://www.tns-counter.ru/V13a****bbdo_ad/ru/UTF-8/tmsec=bbdo_cid1021927-posid1323236/%25aw_random%25', 'audit']], { none => ['tns'] }, $cpm_banner_adgroup));
    ok_validation_result(vr([['https://www.tns-counter.ru/V13a****bbdo_ad/ru/UTF-8/tmsec=bbdo_cid1021927-posid1323236/%aw_random%', 'audit']], { none => ['tns'] }, $cpm_banner_adgroup));
    ok_validation_result(vr([['https://tns-counter.ru/V13a****bbdo_ad/ru/UTF-8/tmsec=bbdo_cid1021927-posid1323236/%aw_random%', 'audit']], { none => ['tns'] }, $cpm_banner_adgroup));
    ok_validation_result(vr([['https://view.adjust.com/impression/yaubk7i?campaign=%5BURU%5DMA_BR-goal_RU-NIZ-NIZ_Video_uber_Aug-19_ios&adgroup=Soc_dem&r=%random%', 'audit']], { none => ['adjust'] }, $cpm_banner_adgroup));
    ok_validation_result(vr([['https://top-fwz1.mail.ru/tracker?id=3129261;e=RG%3A/trg-pixel-5323887-1604422581605;_=%random%', 'audit']], { none => ['mail_ru_top_100'] }, $cpm_banner_adgroup));
    ok_validation_result(vr([['https://tps.doubleverify.com/visit.jpg?ctx=818052&cmp=DV441428&sid=Yandex&plc=20210209001&adsrv=0&btreg=&btadsrv=&crt=&tagtype=&dvtagver=6.1.img&rnd=%random%', 'audit']], { none => ['dv'] }, $cpm_banner_adgroup));
    ok_validation_result(vr([['https://pixel.adsafeprotected.com/?anId=XXXXXX&advId=yyyy&campId=zzzz&placementId=aaaa&rnd=%random%', 'audit']], { none => ['ias'] }, $cpm_banner_adgroup));
    ok_validation_result(vr([['https://impression.appsflyer.com/ru.ozon.app.android?pid=yandexdirect_int&c=promo_video_b2s2021_cpv_rf_view_MRKT-1989&rnd=%random%', 'audit']], { none => ['appsflyer'] }, $cpm_banner_adgroup));
    ok_validation_result(vr([['https://pixel.adlooxtracking.com/ads/ic.php?_=%25aw_random%25&type=pixel&plat=30&tag_id=238&client=weborama&id1=1450&id2=1132&id3=2106&id4=1x1&id5=17136&id6=9&id7=30&id11=&id12=russia&rnd=%random%', 'audit']], { none => ['adloox'] }, $cpm_banner_adgroup));

    ok_validation_result(vr([['https://ad.adriver.ru/cgi-bin/rle.cgi?sid=1&ad=111&bt=21&pid=222&bid=431&bn=431&rnd=%random%', 'audit']], undef, $cpm_deals_adgroup_not_yandex));
    ok_validation_result(vr([['https://ad.adriver.ru/cgi-bin/rle.cgi?sid=1&ad=111&bt=21&pid=222&bid=431&bn=431&rnd=%random%', 'audit']], { private => ['adriver'] }, $cpm_deals_adgroup_not_yandex_with_private));
    ok_validation_result(vr([['https://ad.adriver.ru/cgi-bin/rle.cgi?sid=1&ad=111&bt=21&pid=222&bid=431&bn=431&rnd=%random%', 'audit']], { yandex => ['adriver'] }, $cpm_deals_adgroup_yandex));
    ok_validation_result(vr([['https://bs.serving-sys.com/serving/adserver.bs?cn=display&c=32&pli=1074190221&ord=%random%', 'audit']], undef, $cpm_deals_adgroup_not_yandex));
    ok_validation_result(vr([['https://bs.serving-sys.com/serving/adserver.bs?cn=display&c=32&pli=1074190221&ord=%random%', 'audit']], { private => ['sizmek'] }, $cpm_deals_adgroup_not_yandex_with_private));
    ok_validation_result(vr([['https://bs.serving-sys.com/serving/adserver.bs?cn=display&c=32&pli=1074190221&ord=%random%', 'audit']], { yandex => ['sizmek'] }, $cpm_deals_adgroup_yandex));
    ok_validation_result(vr([['https://wcm.solution.weborama.fr/fcgi-bin/dispatch.fcgi?a.a=im&a.si=7&a.te=3474&a.he=1&a.wi=1&a.hr=p&a.ra=%aw_random%', 'audit']], undef, $cpm_deals_adgroup_not_yandex));
    ok_validation_result(vr([['https://wcm.solution.weborama.fr/fcgi-bin/dispatch.fcgi?a.a=im&a.si=7&a.te=3474&a.he=1&a.wi=1&a.hr=p&a.ra=%aw_random%', 'audit']], { private => ['weborama'] }, $cpm_deals_adgroup_not_yandex_with_private));
    ok_validation_result(vr([['https://wcm.solution.weborama.fr/fcgi-bin/dispatch.fcgi?a.a=im&a.si=7&a.te=3474&a.he=1&a.wi=1&a.hr=p&a.ra=%aw_random%', 'audit']], { yandex => ['weborama'] }, $cpm_deals_adgroup_yandex));
    ok_validation_result(vr([['https://ad.doubleclick.net/ddm/ad/n719887.279382dbmyandexru4863976/b20224086.203459985;sz=728x90;ord=%random%', 'audit']], undef, $cpm_deals_adgroup_not_yandex));
    ok_validation_result(vr([['https://ad.doubleclick.net/ddm/ad/n719887.279382dbmyandexru4863976/b20224086.203459985;sz=728x90;ord=%random%', 'audit']], { private => ['dcm'] }, $cpm_deals_adgroup_not_yandex_with_private));
    ok_validation_result(vr([['https://ad.doubleclick.net/ddm/ad/n719887.279382dbmyandexru4863976/b20224086.203459985;sz=728x90;ord=%random%', 'audit']], { yandex => ['dcm'] }, $cpm_deals_adgroup_yandex));
    ok_validation_result(vr([[AUDIT_GEMIUS_URL, 'audit']], undef, $cpm_deals_adgroup_not_yandex));
    ok_validation_result(vr([[AUDIT_GEMIUS_URL, 'audit']], { private => ['gemius'] }, $cpm_deals_adgroup_not_yandex_with_private));
    ok_validation_result(vr([[AUDIT_GEMIUS_URL, 'audit']], { yandex => ['gemius'] }, $cpm_deals_adgroup_yandex));
    ok_validation_result(vr([['https://www.tns-counter.ru/V13a****bbdo_ad/ru/UTF-8/tmsec=bbdo_cid1021927-posid1323236/%25aw_random%25', 'audit']], undef, $cpm_deals_adgroup_not_yandex));
    ok_validation_result(vr([['https://www.tns-counter.ru/V13a****bbdo_ad/ru/UTF-8/tmsec=bbdo_cid1021927-posid1323236/%aw_random%', 'audit']], undef, $cpm_deals_adgroup_not_yandex));
    ok_validation_result(vr([['https://tns-counter.ru/V13a****bbdo_ad/ru/UTF-8/tmsec=bbdo_cid1021927-posid1323236/%aw_random%', 'audit']], undef, $cpm_deals_adgroup_not_yandex));
    ok_validation_result(vr([['https://www.tns-counter.ru/V13a****bbdo_ad/ru/UTF-8/tmsec=bbdo_cid1021927-posid1323236/%25aw_random%25', 'audit']], { private => ['tns'] }, $cpm_deals_adgroup_not_yandex_with_private));
    ok_validation_result(vr([['https://www.tns-counter.ru/V13a****bbdo_ad/ru/UTF-8/tmsec=bbdo_cid1021927-posid1323236/%aw_random%', 'audit']], { private => ['tns'] }, $cpm_deals_adgroup_not_yandex_with_private));
    ok_validation_result(vr([['https://tns-counter.ru/V13a****bbdo_ad/ru/UTF-8/tmsec=bbdo_cid1021927-posid1323236/%aw_random%', 'audit']], { private => ['tns'] }, $cpm_deals_adgroup_not_yandex_with_private));
    ok_validation_result(vr([['https://www.tns-counter.ru/V13a****bbdo_ad/ru/UTF-8/tmsec=bbdo_cid1021927-posid1323236/%25aw_random%25', 'audit']], { yandex => ['tns'] }, $cpm_deals_adgroup_yandex));
    ok_validation_result(vr([['https://www.tns-counter.ru/V13a****bbdo_ad/ru/UTF-8/tmsec=bbdo_cid1021927-posid1323236/%aw_random%', 'audit']], { yandex => ['tns'] }, $cpm_deals_adgroup_yandex));
    ok_validation_result(vr([['https://tns-counter.ru/V13a****bbdo_ad/ru/UTF-8/tmsec=bbdo_cid1021927-posid1323236/%aw_random%', 'audit']], { yandex => ['tns'] }, $cpm_deals_adgroup_yandex));

    ok_validation_result(vr([['https://mc.yandex.ru/pixel/2555327861230035827?rnd=%random%', 'audience']], { private => ['yndx_audience'] }, $cpm_deals_adgroup_not_yandex_with_private));
    ok_validation_result(vr([['https://mc.yandex.ru/pixel/2555327861230035827?rnd=%random%', 'audience']], { public => ['yndx_audience'] }, $cpm_deals_adgroup_not_yandex));

    foreach my $adgroup ($cpm_deals_adgroup_no_page, $cpm_deals_adgroup_no_page_with_private) {
        ok_validation_result(vr([['https://ad.adriver.ru/cgi-bin/rle.cgi?sid=1&ad=111&bt=21&pid=222&bid=431&bn=431&rnd=%random%', 'audit']], { unknown => ['adriver'] }, $adgroup));

        ok_validation_result(vr([['https://bs.serving-sys.com/serving/adserver.bs?cn=display&c=32&pli=1074190221&ord=%random%', 'audit']], { unknown => ['sizmek'] }, $adgroup));

        ok_validation_result(vr([['https://ad.doubleclick.net/ddm/ad/n719887.279382dbmyandexru4863976/b20224086.203459985;sz=728x90;ord=%random%', 'audit']], { unknown => ['dcm'] }, $adgroup));

        ok_validation_result(vr([[AUDIT_GEMIUS_URL, 'audit']], { unknown => ['gemius'] }, $adgroup));

        ok_validation_result(vr([['https://wcm.solution.weborama.fr/fcgi-bin/dispatch.fcgi?a.a=im&a.si=7&a.te=3474&a.he=1&a.wi=1&a.hr=p&a.ra=%aw_random%', 'audit']], { unknown => ['weborama'] }, $adgroup));
        ok_validation_result(vr([['https://mc.yandex.ru/pixel/2555327861230035827?rnd=%random%', 'audience']], { unknown => ['yndx_audience'] }, $adgroup));
    }
};

done_testing;

