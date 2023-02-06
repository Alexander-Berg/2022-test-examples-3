#!/usr/bin/perl

=pod
    $Id$
=cut

use warnings;
use strict;
use Test::More;

use Yandex::Test::UTF8Builder;

use Common;
use Settings;

use utf8;
use open ':std' => ':utf8';

use Yandex::DBUnitTest qw/:all/;

my %db = (
    ssp_platforms => {
        original_db => PPCDICT,
        rows => [
            { title => 'Valid SSP' },
        ],
    },
);

init_test_dataset(\%db);


*vd = sub { my @errors = Common::validate_domains(@_); @errors ? $errors[0] : '' };

# площадки, которые можно запрещать (из DIRECT-11313)
my @valid_domains = qw/
    blogs.yandex.ru
    images.yandex.ru
    m.pogoda.yandex.ru
    mail.yandex.ru
    maps.yandex.ru
    market.yandex.ru
    narod.ru
    news.yandex.ru
    rasp.yandex.ru
    adresa.yandex.ru
    fotki.yandex.ru
    cards.yandex.ru
    news.yandex.ru
    pogoda.yandex.ru
    search.yaca.yandex.ru
    site.yandex.ru
    tv.yandex.ru
    games.yandex.ru
    slovari.yandex.ru
    music.yandex.ru
    uslugi.yandex.ru
    money.yandex.ru
    rabota.yandex.ru
    my.yandex.ru
/;
is(vd($_), '', $_) for @valid_domains;
is(vd("www.$_"), '', "www.$_") for @valid_domains;

# домены, которые нельзя запрещать (из DIRECT-11313)
my @invalid_domains = qw/
    yandex.ru
    ya.ru
    m.yandex.ru
    mail.ru
    direct.yandex.ru
    yandex.by
    yandex.kz
    yandex.ua
    yandex.com
    m.yandex.com
    ya.com
    direct.yandex.com
    яндекс.рф
    xn--d1acpjx3f.xn--p1ai
/;
isnt(vd($_), '', $_) for @invalid_domains;
isnt(vd("www.$_"), '', "www.$_") for @invalid_domains;

# проверки проверок на длину домена
is(vd('some.normal.domain.name.org'), '', 'normal domain');
isnt(vd('length.of.this.domain.name.is.excactly.255.chars.xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.info'), '', 'domain name with length = 255');
isnt(vd('length.of.this.domain.name.is.more.than.255.chars.xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.info'), '', 'domain name with lenght > 255');

# длинный домен верхнего уровня
is(vd('domain.name.with.long.TLD.musium'), '', 'domain with long TLD part');
is(vd('www.domain.name.with.long.TLD.musium'), '', 'domain with long TLD part with www prefix');

# для доменов общего пользования можно использовать только поддомены
for (qw/pp ac boom msk spb nnov net org com int edu/) {
    isnt(vd("$_.ru"), '', "$_.ru");
    is(vd("subdomain.$_.ru"), '', "subdomain.$_.ru");
    # JavaScript-овая проверка разрешает домены виды www.pp.ru
    # предположительно, они считаются отдельными поддоменами и их также можно [было] зарегистрировать
    is(vd("www.$_.ru"), '', "www.$_.ru");
}

isnt(vd("go.mail.ru"), "", "go.mail.ru");
is(vd("lady.mail.ru"), "", "other mail pages");

is(vd("www.ru"), "", "www.ru");

# валидация с фичей https://st.yandex-team.ru/DIRECT-109732
*vd_feature = sub { my @errors = Common::validate_domains(@_, disable_any_domains_allowed => 1); @errors ? $errors[0] : '' };

@valid_domains = qw/
    blogs.yandex.ru
    images.yandex.ru
    m.pogoda.yandex.ru
    mail.yandex.ru
    maps.yandex.ru
    market.yandex.ru
    narod.ru
    news.yandex.ru
    rasp.yandex.ru
    adresa.yandex.ru
    fotki.yandex.ru
    cards.yandex.ru
    news.yandex.ru
    pogoda.yandex.ru
    search.yaca.yandex.ru
    site.yandex.ru
    tv.yandex.ru
    games.yandex.ru
    slovari.yandex.ru
    music.yandex.ru
    uslugi.yandex.ru
    money.yandex.ru
    rabota.yandex.ru
    my.yandex.ru
    yandex.ru
    ya.ru
    m.yandex.ru
    mail.ru
    direct.yandex.ru
    yandex.by
    yandex.kz
    yandex.ua
    yandex.com
    m.yandex.com
    ya.com
    direct.yandex.com
    яндекс.рф
    xn--d1acpjx3f.xn--p1ai
/;
is(vd_feature($_), '', $_) for @valid_domains;
is(vd_feature("www.$_"), '', "www.$_") for @valid_domains;

# проверки проверок на длину домена
is(vd_feature('some.normal.domain.name.org'), '', 'normal domain');
isnt(vd_feature('length.of.this.domain.name.is.excactly.255.chars.xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.info'), '', 'domain name with length = 255');
isnt(vd_feature('length.of.this.domain.name.is.more.than.255.chars.xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.info'), '', 'domain name with lenght > 255');

# длинный домен верхнего уровня
is(vd_feature('domain.name.with.long.TLD.musium'), '', 'domain with long TLD part');
is(vd_feature('www.domain.name.with.long.TLD.musium'), '', 'domain with long TLD part with www prefix');

# для доменов общего пользования можно использовать только поддомены
for (qw/pp ac boom msk spb nnov net org com int edu/) {
    isnt(vd_feature("$_.ru"), '', "$_.ru");
    is(vd_feature("subdomain.$_.ru"), '', "subdomain.$_.ru");
    # JavaScript-овая проверка разрешает домены виды www.pp.ru
    # предположительно, они считаются отдельными поддоменами и их также можно [было] зарегистрировать
    is(vd_feature("www.$_.ru"), '', "www.$_.ru");
}

is(vd_feature("go.mail.ru"), "", "go.mail.ru");
is(vd_feature("lady.mail.ru"), "", "other mail pages");

is(vd_feature("www.ru"), "", "www.ru");

done_testing;
