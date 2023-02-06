#!/usr/bin/perl

use warnings;
use strict;
use Test::More;

use URLDomain;

use utf8;
use open ':std' => ':utf8';

*pr = sub { URLDomain::predefined_redirect(@_); };

is(pr(undef), undef);
is(pr(''), undef);
is(pr('www.rbc.ru'), undef);
is(pr('http://www.rbc.ru'), undef);

is(pr("click02.begun.ru/click.jsp?url=QrSt-dze394Q7617Zmq2VojUp5KPiiuAYOZwo*NUcqbPNiX9At2h1JdYb1FH0wnxWUP8HKKXfnFATcjbpDihiJNHRwsWrZK7gV9cfspl9Vkq5x-ts8Lrclv3X4ilO57RvHq2yCwFbo6rKAmeK7QZ*RXif30"), undef);
is(
    pr("pixel.everesttech.net/3126/c?ev_sid=90&ev_cmpid=4868670&ev_cl=8a3994cae71fbe88571d4e4682a889c3&ev_lx={param1}&ev_crx={param2}&ev_ln={keyword}&ev_mt={source_type}&ev_src={source}&ev_pos={position}&ev_pt={position_type}&url=http%3A//www.sotmarket.ru/category/karti_pamit.html%3Fref%3D56779%26utm_campaign%3DYandex_K_EN_Sotmarket_Categories_4_Krasnodar_network%26utm_medium%3Dcpc%26utm_source%3Dicontextt%26utm_term%3Dkupit_kartu_pamyati_sdhc"),
    "http://www.sotmarket.ru/category/karti_pamit.html?ref=56779&utm_campaign=Yandex_K_EN_Sotmarket_Categories_4_Krasnodar_network&utm_medium=cpc&utm_source=icontextt&utm_term=kupit_kartu_pamyati_sdhc"
    );
is(
    pr("pixel.everesttech.net/3126/c?ev_sid=90&ev_cmpid=4868670&ev_cl=8a3994cae71fbe88571d4e4682a889c3&ev_lx={param1}&ev_crx={param2}&ev_ln={keyword}&ev_mt={source_type}&ev_src={source}&ev_pos={position}&ev_pt={position_type}&url=!http://www.sotmarket.ru/?safd=ewqr&asdf=qwetgsd"),
    "http://www.sotmarket.ru/?safd=ewqr&asdf=qwetgsd"
    );
is(
    pr("http://pixel.everesttech.net/2724/c?ev_pt={position_type}&url=http%3A%2F%2Fwww.%D0%9E%D0%B4%D0%B5%D0%B6%D0%B4%D0%B0%2B%D0%BD%D0%B0%D0%B2%D0%B5%D1%81%D0%BD%D1%83%D0%B4%D0%BB%D1%8F%D0%B4%D0%B5%D0%B2%D0%BE%D1%87%D0%B5%D0%BA%3Futm_source%3DYDirect%26utm_medium%3Dcpc%26utm_campaign%3D6066859.Category%2BOd%2BVesennyaya%2BOdejda%2BRegions%26utm_term%3DPHR.{param1}.146353096"),
    undef
    );

done_testing();
