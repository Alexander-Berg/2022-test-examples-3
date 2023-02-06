#!/usr/bin/perl

# $Id$

use warnings;
use strict;
use Test::More;

use ModerateChecks;

use utf8;
use open ':std' => ':utf8';

use Data::Dumper;
$Data::Dumper::Sortkeys = 1;

*cma = sub { return check_moderate_banner(@_) };

my $bo = get_banner();

# nothing changed
is(cma($bo,$bo), 0, 'nothing changed');

# body changed
is(cma(get_banner(change_body=>1), $bo), 1, 'body changed');

# body changed slightly
is(cma(get_banner(change_body_slightly=>1), $bo), 0, 'body changed slightly');

# body changed sematic Купите наши кондиционеры - > КУПИТЕ наш кондиционер
is(cma(get_banner(change_body_semantic=>1), $bo), 1, 'body changed semantic');

# change domain
is(cma(get_banner(change_domain=>1), $bo), 1, "change only domain (not href)");

# change href
is(cma(get_banner(change_href => 1), $bo), 1, "change href");

# change href and domain
is(cma(get_banner(change_href => 1, change_domain => 1), $bo), 1, "change href and domain");

# delete href
is(cma(get_banner(delete_href=>1), $bo), 0, "delete href");

# одинаковые мобильные баннеры без href (без трекинговой ссылки)
my $mob_banner_1 = get_banner(delete_href => 1);
my $mob_banner_2 = get_banner(delete_href => 1);
is(cma($mob_banner_1, $mob_banner_2), 0, 'mobile banners');

my $mob_banner_3 = get_banner(delete_href => 1);
my $mob_banner_4 = get_banner(delete_href => 1, change_body => 1);
is(cma($mob_banner_3, $mob_banner_4), 1);

done_testing;


sub get_banner
{
    my %OPT = @_;

    my %BASE_BANNER = (
        real_bannet_type => 'text',
        'bid' => '4949585',
        'body' => "Купите наши кондиционеры",
        'domain' => 'www.barnesandnoble.com',
        'domain_ascii' => 'www.barnesandnoble.com',
        'href' => 'books.com',
        'statusModerate' => 'Yes',
        'title' => "Холодильник у вас в велике",
        'phone' => '123-45-67',
    );

    my %b = %BASE_BANNER;
    
    if ($OPT{change_body}) {
        $b{body}.='1';
    }

    if ($OPT{change_body_semantic}) {
        $b{body} = 'КУПИТЕ наш кондиционер';
    }

    if ($OPT{change_body_slightly}){
        $b{body} = "    ".$b{body}." ";
    }

    if ($OPT{change_domain}) {
        $b{domain} = 'www.ya.ru';
    }

    if ($OPT{change_href}) {
        $b{domain} = 'www.ya.ru/yandsearch';
    }

    if ($OPT{delete_href}) {
        delete $b{href};
        delete $b{domain};
    }

    return \%b;
}
