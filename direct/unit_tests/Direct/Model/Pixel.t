#!/usr/bin/env perl

use Direct::Modern;

use Test::More;
use Test::Exception;
use Test::Deep;

BEGIN {
    use_ok('Direct::Model::Pixel');
}

use constant AUDIENCE_URL => 'https://mc.yandex.ru/pixel/2555327861230035827?rnd=%aw_random%';
use constant AUDIT_ADFOX_URL => 'https://ads.adfox.ru/254364/getCode?p1=bxoar&p2=v&pfc=bnkqk&pfb=failv&pr=%25aw_RANDOM%25&ptrc=b';
use constant AUDIT_ADFOX_ADMETRICA_URL => 'https://amc.yandex.ru/show?cmn_id=4&plt_id=4&crv_id=4&evt_tp=impression&ad_type=banner&vv_crit=mrc&rnd=%Random%';
use constant AUDIT_ADFOX_MC_ADMETRICA_URL => 'https://mc.admetrica.ru/show?cmn_id=4&plt_id=4&crv_id=4&evt_tp=impression&ad_type=banner&vv_crit=mrc&rnd=%Random%';
use constant AUDIT_TNS_URL => 'https://www.tns-counter.ru/V13a****bbdo_ad/ru/UTF-8/tmsec=bbdo_cid1021927-posid1323236/%25aw_random%25';
use constant AUDIT_ADRIVER_URL => 'https://ad.adriver.ru/cgi-bin/rle.cgi?sid=1&ad=111&bt=21&pid=222&bid=431&bn=431&rnd=%random%';
use constant AUDIT_SIZMEK_URL => 'https://bs.serving-sys.com/Serving/adServer.bs?cn=display&c=32&pli=1074190221&ord=%random%';
use constant AUDIT_WEBORAMA_URL => 'https://wcm.solution.weborama.fr/fcgi-bin/dispatch.fcgi?a.A=im&a.si=7&a.te=3474&a.he=1&a.wi=1&a.hr=p&a.ra=%aw_random%';
use constant AUDIT_WEBORAMA_RU_URL => 'https://wcm-ru.frontend.weborama.fr/fcgi-bin/dispatch.fcgi?a.A=im&a.si=5364&a.te=15096&a.he=1&a.wi=1&a.hr=p&a.ra=%random%';
use constant AUDIT_DCM_URL => 'https://ad.doubleclick.net/ddm/ad/N719887.279382DBMYANDEXRU4863976/B20224086.203459985;sz=728x90;ord=%random%';
use constant AUDIT_GEMIUS_URL => 'https://gdeby.hit.gemius.pl/_%aw_random%/redot.gif?id=bVdLcgtpfJcK8F5yqvK7naP8zUsi2I7Q5bo9ydeoYoP.p7/fastid=igjvgufcuuhxoigmzdimcqemvblf/stparam=zaolqinkmf';
use constant AUDIT_MEDIASCOPE_URL => 'https://verify.yandex.ru/verify?platformid=1&customdata=jfjfjfjfhfuueue&DRND=%random%';
use constant AUDIT_OMI_URL => 'https://verify.yandex.ru/verify?platformid=9';
use constant AUDIT_ADJUST_URL => 'https://view.adjust.com/impression/yaubk7i?campaign=_R-goal_RU-NIZ-NIZ_Video_uber_Aug-19_ios&RND=%random%';
use constant AUDIT_MAILRU_URL => 'https://top-fwz1.mail.ru/tracker?id=3129261;e=RG%3A/trg-pixel-5323887-1604422581605;_=%random%';
use constant AUDIT_DV_URL => 'https://tps.doubleverify.com/visit.jpg?ctx=818052&cmp=DV441428&sid=Yandex&plc=20210209001&adsrv=0&btreg=&btadsrv=&crt=&tagtype=&dvtagver=6.1.img&rnd=%random%';
use constant AUDIT_IAS_URL => 'https://pixel.adsafeprotected.com/?anId=XXXXXX&advId=yyyy&campId=zzzz&placementId=aaaa&rnd=%random%';
use constant AUDIT_APPSFLYER_URL => 'https://impression.appsflyer.com/ru.ozon.app.android?pid=yandexdirect_int&c=promo_video_b2s2021_cpv_rf_view_MRKT-1989&rnd=%random%';
use constant AUDIT_ADLOOX_URL => 'https://pixel.adlooxtracking.com/ads/ic.php?_=%25aw_random%25&type=pixel';

use constant AUDIT_PROVIDER_URLS => [AUDIT_ADFOX_URL, AUDIT_ADFOX_ADMETRICA_URL, AUDIT_ADFOX_MC_ADMETRICA_URL, AUDIT_TNS_URL, AUDIT_ADRIVER_URL, AUDIT_SIZMEK_URL,
    AUDIT_WEBORAMA_URL, AUDIT_WEBORAMA_RU_URL, AUDIT_DCM_URL, AUDIT_MEDIASCOPE_URL, AUDIT_ADJUST_URL, AUDIT_MAILRU_URL, AUDIT_DV_URL, AUDIT_IAS_URL, AUDIT_APPSFLYER_URL, AUDIT_ADLOOX_URL];

sub mk_pixel { Direct::Model::Pixel->new(@_) }

subtest "Model" => sub {
    lives_ok { mk_pixel() };
    lives_ok { mk_pixel(url => AUDIENCE_URL, kind => 'audience') };
    lives_ok { mk_pixel(url => AUDIT_MEDIASCOPE_URL, kind => 'audit', campaign_id => 456, adgroup_type => 'cpm_video') };
    dies_ok { mk_pixel(url => AUDIT_MEDIASCOPE_URL, kind => 'audit', campaign_id => 456, adgroup_type => 'wrong_type') };
    dies_ok { mk_pixel(url => AUDIENCE_URL, kind => 'audience', invalid_key => 1) };
    dies_ok { mk_pixel(url => 'http://ya.ru', kind => 'wrong') };
};

subtest "uniq_key" => sub {
    my $banner_id = 20;
    is mk_pixel(banner_id => $banner_id, url => AUDIENCE_URL, kind => 'audience')->uniq_key, join(',', $banner_id, AUDIENCE_URL);
    foreach my $url (@{AUDIT_PROVIDER_URLS()}) {
        is mk_pixel(banner_id => ++$banner_id, url => $url, kind => 'audit')->uniq_key, join(',', $banner_id, $url);
    }
};

subtest "is_provider_matches_kind" => sub {
    ok mk_pixel(url => AUDIENCE_URL, kind => 'audience')->is_provider_matches_kind;
    ok !mk_pixel(url => AUDIENCE_URL, kind => 'audit')->is_provider_matches_kind;

    foreach my $url (@{AUDIT_PROVIDER_URLS()}) {
        ok mk_pixel(url => $url, kind => 'audit')->is_provider_matches_kind;
        ok !mk_pixel(url => $url, kind => 'audience')->is_provider_matches_kind;
    }
};

subtest "is_valid_provider_url" => sub {
    ok mk_pixel(url => AUDIENCE_URL, kind => 'audience')->is_valid_provider_url;
    ok !mk_pixel(url => 'h'.AUDIENCE_URL, kind => 'audience')->is_valid_provider_url;

    foreach my $url (@{AUDIT_PROVIDER_URLS()}) {
        ok mk_pixel(url => $url, kind => 'audit')->is_valid_provider_url;
        ok !mk_pixel(url => 'h'.$url, kind => 'audience')->is_valid_provider_url;
    }

    ok !mk_pixel(url => 'https://wcm-ru.solution.weborama.fr/fcgi-bin/dispatch.fcgi?a.A=im&a.si=5364&a.te=15096&a.he=1&a.wi=1&a.hr=p&a.ra=%random%', kind => 'audience')->is_valid_provider_url;
};

subtest "is_kind_audience" => sub {
    ok mk_pixel(url => AUDIENCE_URL, kind => 'audience')->is_kind_audience;
    foreach my $url (@{AUDIT_PROVIDER_URLS()}) {
        ok !mk_pixel(url => $url, kind => 'audit')->is_kind_audience;
    }
};

subtest "to_template_hash" => sub {
    my $template_hash = mk_pixel(id => 1, url => AUDIENCE_URL, kind => 'audience')->to_template_hash;
    cmp_deeply($template_hash, {id => 1, kind => 'audience', provider => 'yndx_audience', url => AUDIENCE_URL});
};

subtest "url_with_subst_random" => sub {
    is mk_pixel(url => AUDIENCE_URL, kind => 'audience')->url_with_subst_random, 'https://mc.yandex.ru/pixel/2555327861230035827?rnd={DRND}';
    is mk_pixel(url => AUDIT_ADFOX_URL, kind => 'audit')->url_with_subst_random, 'https://ads.adfox.ru/254364/getCode?p1=bxoar&p2=v&pfc=bnkqk&pfb=failv&pr={DRND}&ptrc=b&b_id={BID}&c_id={HLID}&o_id={OID}';
    is mk_pixel(url => AUDIT_ADFOX_ADMETRICA_URL, kind => 'audit')->url_with_subst_random, 'https://amc.yandex.ru/show?cmn_id=4&plt_id=4&crv_id=4&evt_tp=impression&ad_type=banner&vv_crit=mrc&rnd={DRND}&b_id={BID}&c_id={HLID}&o_id={OID}';
    is mk_pixel(url => AUDIT_ADFOX_MC_ADMETRICA_URL, kind => 'audit')->url_with_subst_random, 'https://mc.admetrica.ru/show?cmn_id=4&plt_id=4&crv_id=4&evt_tp=impression&ad_type=banner&vv_crit=mrc&rnd={DRND}&b_id={BID}&c_id={HLID}&o_id={OID}';
    is mk_pixel(url => AUDIT_TNS_URL, kind => 'audit')->url_with_subst_random, 'https://www.tns-counter.ru/V13a****bbdo_ad/ru/UTF-8/tmsec=bbdo_cid1021927-posid1323236/{DRND}';
    is mk_pixel(url => AUDIT_ADRIVER_URL, kind => 'audit')->url_with_subst_random, 'https://ad.adriver.ru/cgi-bin/rle.cgi?sid=1&ad=111&bt=21&pid=222&bid=431&bn=431&rnd={DRND}';
    is mk_pixel(url => AUDIT_WEBORAMA_URL, kind => 'audit')->url_with_subst_random, 'https://wcm.solution.weborama.fr/fcgi-bin/dispatch.fcgi?a.A=im&a.si=7&a.te=3474&a.he=1&a.wi=1&a.hr=p&a.ra={DRND}';
    is mk_pixel(url => AUDIT_WEBORAMA_RU_URL, kind => 'audit')->url_with_subst_random, 'https://wcm-ru.frontend.weborama.fr/fcgi-bin/dispatch.fcgi?a.A=im&a.si=5364&a.te=15096&a.he=1&a.wi=1&a.hr=p&a.ra={DRND}';
    is mk_pixel(url => AUDIT_SIZMEK_URL, kind => 'audit')->url_with_subst_random, 'https://bs.serving-sys.com/Serving/adServer.bs?cn=display&c=32&pli=1074190221&ord={DRND}';
    is mk_pixel(url => AUDIT_DCM_URL, kind => 'audit')->url_with_subst_random, 'https://ad.doubleclick.net/ddm/ad/N719887.279382DBMYANDEXRU4863976/B20224086.203459985;sz=728x90;ord={DRND}';
    is mk_pixel(url => AUDIT_GEMIUS_URL, kind => 'audit')->url_with_subst_random, 'https://gdeby.hit.gemius.pl/_{DRND}/redot.gif?id=bVdLcgtpfJcK8F5yqvK7naP8zUsi2I7Q5bo9ydeoYoP.p7/fastid=igjvgufcuuhxoigmzdimcqemvblf/stparam=zaolqinkmf';
};

subtest 'mediascope_omi_url_with_subst_rando' => sub {
    is
        mk_pixel(url => AUDIT_MEDIASCOPE_URL, kind => 'audit', banner_id => 123, campaign_id => 773, adgroup_type => 'cpm_video')->url_with_subst_random,
        'https://verify.yandex.ru/verify?platformid=1&customdata=jfjfjfjfhfuueue&DRND={DRND}&BID=123&BTYPE=1&CID=773&DRND={DRND}&DTYPE={DEVICE_TYPE}&REF={TREF}&SESSION={Kad-session-id}&hitlogid={HLID}&page={PAGE}';

    is
        mk_pixel(url => AUDIT_MEDIASCOPE_URL, kind => 'audit', banner_id => 123, campaign_id => 773, adgroup_type => 'cpm_banner')->url_with_subst_random,
        'https://verify.yandex.ru/verify?platformid=1&customdata=jfjfjfjfhfuueue&DRND={DRND}&BID=123&BTYPE=2&CID=773&DRND={DRND}&DTYPE={DEVICE_TYPE}&REF={TREF}&SESSION={Kad-session-id}&hitlogid={HLID}&page={PAGE}';

    is
        mk_pixel(url => AUDIT_MEDIASCOPE_URL, kind => 'audit', banner_id => 123, campaign_id => 773, adgroup_type => 'cpm_yndx_frontpage')->url_with_subst_random,
        'https://verify.yandex.ru/verify?platformid=1&customdata=jfjfjfjfhfuueue&DRND={DRND}&BID=123&BTYPE=0&CID=773&DRND={DRND}&DTYPE={DEVICE_TYPE}&REF={TREF}&SESSION={Kad-session-id}&hitlogid={HLID}&page={PAGE}';

    my $url_without_rand = 'https://verify.yandex.ru/verify?platformid=1';
    is
        mk_pixel(url => $url_without_rand, kind => 'audit', banner_id => 123, campaign_id => 773, adgroup_type => 'cpm_yndx_frontpage')->url_with_subst_random,
        'https://verify.yandex.ru/verify?platformid=1&BID=123&BTYPE=0&CID=773&DRND={DRND}&DTYPE={DEVICE_TYPE}&REF={TREF}&SESSION={Kad-session-id}&hitlogid={HLID}&page={PAGE}';

    my $url_with_rand = 'https://verify.yandex.ru/verify?rand=%random%&platformid=1';
    is
        mk_pixel(url => $url_with_rand, kind => 'audit', banner_id => 123, campaign_id => 773, adgroup_type => 'cpm_yndx_frontpage')->url_with_subst_random,
        'https://verify.yandex.ru/verify?rand={DRND}&platformid=1&BID=123&BTYPE=0&CID=773&DRND={DRND}&DTYPE={DEVICE_TYPE}&REF={TREF}&SESSION={Kad-session-id}&hitlogid={HLID}&page={PAGE}';    

    is
        mk_pixel(url => AUDIT_OMI_URL, kind => 'audit', banner_id => 123, campaign_id => 773, adgroup_type => 'cpm_video')->url_with_subst_random,
        'https://verify.yandex.ru/verify?platformid=9&BID=123&BTYPE=1&CID=773&DRND={DRND}&DTYPE={DEVICE_TYPE}&REF={TREF}&SESSION={Kad-session-id}&hitlogid={HLID}&page={PAGE}';

    is
        mk_pixel(url => AUDIT_OMI_URL, kind => 'audit', banner_id => 123, campaign_id => 773, adgroup_type => 'cpm_banner')->url_with_subst_random,
        'https://verify.yandex.ru/verify?platformid=9&BID=123&BTYPE=2&CID=773&DRND={DRND}&DTYPE={DEVICE_TYPE}&REF={TREF}&SESSION={Kad-session-id}&hitlogid={HLID}&page={PAGE}';

    is
        mk_pixel(url => AUDIT_OMI_URL, kind => 'audit', banner_id => 123, campaign_id => 773, adgroup_type => 'cpm_yndx_frontpage')->url_with_subst_random,
        'https://verify.yandex.ru/verify?platformid=9&BID=123&BTYPE=0&CID=773&DRND={DRND}&DTYPE={DEVICE_TYPE}&REF={TREF}&SESSION={Kad-session-id}&hitlogid={HLID}&page={PAGE}';

};

*build_mediascope_audit_pixel = \&Direct::Model::Pixel::build_mediascope_audit_pixel;
subtest "mediascope_autointegration_pixel" => sub {
    is
        build_mediascope_audit_pixel("sec_pref_18", 10, 78, 'cpm_banner')->url,
        "https://{DRND}.verify.yandex.ru/verify?platformid=1&msid=sec_pref_18_5-10-78";
    is
        build_mediascope_audit_pixel('', 555, 3, 'cpm_banner')->url,
        "https://{DRND}.verify.yandex.ru/verify?platformid=1&msid=_5-555-3";
};

*build_omi_audit_pixel = \&Direct::Model::Pixel::build_omi_audit_pixel;
subtest "omi_autointegration_pixel" => sub {
    is
        build_omi_audit_pixel(10, 78, 'cpm_banner')->url,
        "https://verify.yandex.ru/verify?platformid=9";
};

subtest "need_to_check_provider_permission" => sub {
    ok !mk_pixel(url => AUDIENCE_URL, kind => 'audience')->need_to_check_provider_permission('cpm_banner');
    foreach my $url (@{AUDIT_PROVIDER_URLS()}) {
        my $result = mk_pixel(url => $url, kind => 'audit')->need_to_check_provider_permission('cpm_banner');
        if ($url eq AUDIT_ADFOX_URL || $url eq AUDIT_ADFOX_ADMETRICA_URL || $url eq AUDIT_ADFOX_MC_ADMETRICA_URL) {
            ok !$result;
        } else {
            ok $result;
        }
    }

    foreach my $is_yandex_page (undef, 0, 1) {
        foreach my $has_private_criterion (0, 1) {
            my $result = mk_pixel(url => AUDIENCE_URL, kind => 'audience')->need_to_check_provider_permission('cpm_deals', $is_yandex_page, $has_private_criterion);
            if ($is_yandex_page) {
                ok !$result;
            } else {
                ok $result;
            }

            foreach my $url (@{AUDIT_PROVIDER_URLS()}) {
                my $result = mk_pixel(url => $url, kind => 'audit')->need_to_check_provider_permission('cpm_deals', $is_yandex_page, $has_private_criterion);
                if ($url eq AUDIT_ADFOX_URL || $url eq AUDIT_ADFOX_ADMETRICA_URL || $url eq AUDIT_ADFOX_MC_ADMETRICA_URL || defined $is_yandex_page && !$is_yandex_page && !$has_private_criterion) {
                    ok !$result;
                } else {
                    ok $result;
                }
            }
        }
    }
};

subtest "get_accessible_audit_provider_names" => sub {
    cmp_deeply mk_pixel(url => AUDIENCE_URL, kind => 'audience')->get_accessible_audit_provider_names('cpm_banner', undef, undef, []), [qw/Adfox/];
    cmp_deeply mk_pixel(url => AUDIENCE_URL, kind => 'audience')->get_accessible_audit_provider_names('cpm_banner', undef, undef, [qw/adriver weborama sizmek dcm tns/]),
        [qw/Adfox Adriver DCM Sizmek TNS Weborama/];
    cmp_deeply mk_pixel(url => AUDIENCE_URL, kind => 'audience')->get_accessible_audit_provider_names('cpm_deals', undef, 0, []), [qw/Adfox/];
    cmp_deeply sort(mk_pixel(url => AUDIENCE_URL, kind => 'audience')->get_accessible_audit_provider_names('cpm_deals', 0, 0, [])),
        [sort(qw/Adfox Adjust Adriver DCM Gemius Mediascope OMI Sizmek TNS Weborama DoubleVerify IAS AppsFlyer adloox/, "Mail.ru top-100")];
};

done_testing;
