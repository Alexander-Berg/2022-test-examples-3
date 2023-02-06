#!/usr/bin/perl

=pod

    $Id$

=cut

use Direct::Modern;
use Test::More;

use URLDomain;

*vbh = sub { !validate_banner_href(@_); };

foreach my $method ( qw(output failure_output) ) { # здесь почему-то старый Test::Builder, который не хочет utf в названии тесткейса
    binmode Test::More->builder->$method(), ':encoding(UTF-8)';
}

ok(!vbh("www.rbc.ru"));
ok(!vbh("www.rbc.ru/sadf/sdf.html?asdfasdf&sdaf=asdf#asdfsad"));
ok(!vbh("www.rbc.ru#sdf.html?asdfasdf&sdaf=asdfasdfsad"));
ok(!vbh("www.rbc.jhlkjhhru"));

ok(vbh("https://www.rbc.ru#jhlkjhhru"), "якорь без слеша");
ok(vbh("https://www.rbc.ru?a=b#jhlkjhhru"), "якорь без слеша после ?");
ok(!vbh("https://www.rbc.ru#jhl?kjh=hru"), "якорь до первого ? без слеша");
ok(!vbh("http://www.rbc.ru{keyword}#sdf.html"), "с якорем без слеша и ?, зато с параметром в {} перед якорем");
ok(vbh("http://www.rbc.ru/#keyword#sdf.html?asdfasdf&sdaf=asdfasdfsad"), "c шаблоном в пути");
ok(!vbh("http://www.rbc.ru#keyword#sdf.html?asdfasdf&sdaf=asdfasdfsad"), "c неприметным шаблоном в домене");
ok(!vbh("https://Myetherwallet.com#keyword#s1.Drolcoma.com/vacancy/?utm_term={keyword}&utm_campaign={region_name}&id=ul81rt"), "c вполне приметным шаблоном в домене");
ok(!vbh("http://udochka2.ohota-24{ad_id}.ru/#reviews?i=2?utm_source=rsya&utm_medium=cpc&utm_campaign={campaign_id}&utm_content=bs2_{ad_id}&utm_term=удочка"), "национальные символы в get-параметрах, но ещё и параметр в домене");
ok(!vbh("http://www.rbc.ru#keyword/lalala#sdf.html?asdfasdf&sdaf=asdfa"), "без шаблонов, просто разломанный href");
ok(!vbh("http://ya}keyword.ru/test"), "с единичной фигурной скобкой до слеша");
ok(vbh("http://ya.ru?test={phrase_id}"), "с подстановочным параметром после ? без слеша");
ok(vbh("http://ya.ru/?test={phrase_id}"), "с подстановочным параметром после /");

ok(vbh("http://www.rbc.ru"));
ok(vbh("https://www.rbc.ru"));
ok(!vbh("https://www.rbc.ru sdfgsdfg"));

ok(!vbh("http://127.0.0.1:80/2313"));
ok(vbh("http://127.0.0.1:80/2313", {allow_ip => 1}));
ok(!vbh("127.0.0.1:80/2313", {allow_ip => 1}));

ok(vbh("http://правительство.рф"));
ok(vbh("http://правительство.онлайн"));
ok(vbh("http://правительство.оффлайн"));
ok(vbh("http://xn--d1abbgf6aiiy.xn--p1ai"));

ok(!vbh("0"));
ok(!vbh("12345"));

# пустой урл допустим (убрать нельзя, такое поведение требуется при создании кампании)
ok(vbh(""));
ok(vbh(undef));

# ссылки с разными необычными символами: () [] {} |
ok(vbh("http://www.intel.com/ru_ru/business/itcenter/index.htm?cid=emea:yan|vprodtop_ru_desktop|ru891DE|s"));
ok(vbh("http://ad.doubleclick.net/clk;228044917;38592682;t?http://www.klm.com/travel/ru_ru/index.htm?popup=no&WT.srch=1&WT.vr.rac_of=1&WT.vr.rac_ma=RU%20KLM%20Branding&WT.vr.rac_cr=KLM&WT.seg_1={keyword:nil}&WT.vr.rac_pl=RU&WT.vr.rac_pa=Yandex&WT.mc_id=2213229|3468187|38592682|228044917|785093&WT.srch=1"));
ok(vbh("http://ad.doubleclick.net/clk;228044917;38592682;t?http://www.klm.com/travel/ru_ru/index.htm?popup=no&WT.srch=1&WT.vr.rac_of=1&WT.vr.rac_ma=RU%20KLM%20Branding&WT.vr.rac_cr=KLM&WT.seg_1={keyword:nil}&WT.vr.rac_pl=RU&WT.vr.rac_pa=Yandex&WT.mc_id=2213229|3468187|38592682|228044917|785093&WT.srch=1"));
ok(vbh("http://www.adtraction.com/t/t?a=102378751&as=50941721&t=2&tk=1&epi=(11!0!вытяжки!0)&url=http://electrolux.ru/node36.aspx?categoryid=9105"));
ok(vbh("http://www.adtraction.com/t/t?a=102378751&as=50941721&t=2&tk=1&epi=(11!0!вытяжки!0)&url=http://electrolux.ru/node36.aspx?categoryid=9105"));
ok(vbh("http://www.klm.com/travel/ru_ru/apps/ebt/ebt_home.htm?c[0].ds=ZRH&WT.srch=1&WT.vr.rac_of=1&WT.vr.rac_ma=Yandex&WT.vr.rac_cr=Zurich&WT.seg_1={keyword:nil}&WT.vr.rac_pl=RU&WT.vr.rac_pa=Google&WT.mc_id=2213229|3468187|38592682|228044917|785093&WT.srch=1"));
ok(vbh("http://www.vsedlyauborki.ru/catalog/5/25#Derjateli_shubok_dlya_myt'ya_okon"));

ok(!vbh("\nhttps://direct.yandex.ru"));
ok(!vbh("https://direct.yandex.ru\n"));

# длина поддоменов
ok(vbh("http://landing.compan/"));
ok(vbh("http://landing.international/"));
# домен первого уровня до 63 [a-z] символов
ok(vbh("http://landingqweasdzxcrqweasdzxcrqweazxcrqweasdzxcrqweasdzxcrqweasdz.crooo/"));
ok(!vbh("http://landingqweasdzxcrqweasdzxcrqweasdzxcrqweasdzxcrqweasdzxcrqweasdzxc.roooz/"));
ok(vbh("http://landing.qwertyuiopasdfg/", {allow_http => 1}));
ok(!vbh("http://landing.qwertyuiopasdfgh/", {allow_http => 1}));

ok(vbh("http://domain.a123/", {allow_http => 1}));
ok(!vbh("http://domain.123/", {allow_http => 1}));
ok(!vbh("http://domain.123domain/", {allow_http => 1}));
ok(!vbh("http://domain.a123-sub/", {allow_http => 1}));

done_testing();
