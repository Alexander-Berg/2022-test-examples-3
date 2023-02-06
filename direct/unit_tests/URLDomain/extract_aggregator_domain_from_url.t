#!/usr/bin/perl

#    $Id: get_url_domain.t 162882 2018-08-01 13:37:24Z a-lobanova $

use strict;
use warnings;
use utf8;

use Test::More;
use Yandex::Test::UTF8Builder;


BEGIN { use_ok('AggregatorDomains'); }

my @test_data = (
    {
        href => undef,
        expected_domain => undef, 
    },
    {
        href => '',
        expected_domain => undef, 
    },
    {
        href => 'http://vk.com/test',
        expected_domain => 'test.vk.com', 
    },
    {
        href => 'http://vK.cOm/TeSt',
        expected_domain => 'test.vk.com', 
    },
    {
        href => 'https://vk.com/test',
        expected_domain => 'test.vk.com', 
    },
    {
        href => 'http://vk.com/test?param=1',
        expected_domain => 'test.vk.com', 
    },
    {
        href => 'http://vk.com/test2#results',
        expected_domain => 'test2.vk.com', 
    },
    {
        href => 'http://vk.com/test1/test2/?param=1#results',
        expected_domain => 'test1.vk.com', 
    },
    {
        href => 'https://vk.com/part1/part2/part3/part4',
        expected_domain => 'part1.vk.com', 
    },
    {
        href => 'https://vk.com/',
        expected_domain => undef, 
    },
    {
        href => 'https://vk.com',
        expected_domain => undef, 
    },
    {
        href => 'https://',
        expected_domain => undef, 
    },
    {
        href => 'http://vk.com:8012/test?param=1',
        expected_domain => 'test.vk.com', 
    },
    {
        href => 'http://m.vk.com/test',
        expected_domain => 'test.vk.com', 
    },
    {
        href => 'http://part1.part2.part3.vk.com:8012/test1/test2',
        expected_domain => 'test1.vk.com', 
    },
    {
        href => 'http://notvk.com/test',
        expected_domain => undef, 
    },
    {
        href => 'http://com/test',
        expected_domain => undef, 
    },
    {
        href => 'http://vk.com//test',
        expected_domain => undef, 
    },
    {
        href => 'http://vk.com/test/',
        expected_domain => 'test.vk.com', 
    },
    {
        href => 'http://vk.com/test1/test2',
        expected_domain => 'test1.vk.com', 
    },
    { # замена недопустимых символов на -
        href => 'https://vk.com/part1_part2',
        expected_domain => 'part1-part2.vk.com', 
    },
    { 
        href => 'https://vk.com/part1_part2__part3___part4-part5--part6_-_part7/',
        expected_domain => 'part1-part2-part3-part4-part5-part6-part7.vk.com', 
    },
    {
        href => 'https://vk.com/-.-part1_part2_..-part3-_._/',
        expected_domain => 'part1-part2-part3.vk.com', 
    },
    {
        href => 'https://vk.com/-_-_/test/',
        expected_domain => undef, 
    },
    {
        href => 'http://VK.COM/Русские_Буквы',
        expected_domain => 'русские-буквы.vk.com', 
    },
    { # длина больше максимальной
        href => 'https://vk.com/' . ('z' x 64),
        expected_domain => undef, 
    },

    # instagram.com
    {
        href => 'https://www.instagram.com',
        expected_domain => undef, 
    },
    {
        href => 'http://instagram.com/',
        expected_domain => undef, 
    },
    {
        href => 'https://www.instagram.com/test/',
        expected_domain => 'test.instagram.com', 
    },
    {
        href => 'https://www.instagram.com/..test1.test2-test3--/test4/?param=1#test',
        expected_domain => 'test1-test2-test3.instagram.com', 
    },

    # ok.ru
    {
        href => 'http://ok.ru',
        expected_domain => undef, 
    },
    {
        href => 'http://ok.ru/',
        expected_domain => undef, 
    },
    {
        href => 'http://ok.ru/profile/123456789',
        expected_domain => 'profile-123456789.ok.ru', 
    },
    {
        href => 'http://ok.ru/profile/123456789/test1/test2?param=1',
        expected_domain => 'profile-123456789.ok.ru', 
    },
    {
        href => 'https://ok.ru/group/1234567',
        expected_domain => 'group-1234567.ok.ru', 
    },
    {
        href => 'https://www.ok.ru/group1234567',
        expected_domain => 'group-1234567.ok.ru', 
    },
    {
        href => 'https://www.ok.ru/group1234567/test1/test2#test3',
        expected_domain => 'group-1234567.ok.ru', 
    },
    {
        href => 'https://www.ok.ru/testgroup1234567/',
        expected_domain => 'testgroup1234567.ok.ru', 
    },
    {
        href => 'https://www.ok.ru/..test1.test2-test3--/test4/?param=1#test',
        expected_domain => 'test1-test2-test3.ok.ru', 
    },
    {
        href => 'http://ok.ru/-..-/',
        expected_domain => undef, 
    },
    {
        href => 'http://ok.ru/1234',
        expected_domain => '1234.ok.ru', 
    },
    { # проверка на минимальную длину, чтобы не расклеивать ссылки вида http://ok.ru/dk?cmd=..
        href => 'http://ok.ru/123',
        expected_domain => undef, 
    },

    # youtube.com
    {
        href => 'https://www.youtube.com',
        expected_domain => undef, 
    },
    {
        href => 'http://youtube.com/',
        expected_domain => undef, 
    },
    {
        href => 'https://www.youtube.com/watch?v=TeSt123',
        expected_domain => undef, 
    },
    {
        href => 'https://www.youtube.com/channel/tEsT1234',
        expected_domain => 'channel-test1234.youtube.com', 
    },
    {
        href => 'https://www.youtube.com/channel/Test1_tEst2-test3/test4?param=1',
        expected_domain => 'channel-test1-test2-test3.youtube.com', 
    },
    {
        href => 'https://www.youtube.com/channel/',
        expected_domain => undef, 
    },

    # sites.google.com
    {
        href => 'https://sites.google.com',
        expected_domain => undef, 
    },
    {
        href => 'https://sites.google.com/',
        expected_domain => undef, 
    },
    {
        href => 'https://sites.google.com/site/test',
        expected_domain => 'test-site.sites.google.com', 
    },
    {
        href => 'http://www.sites.google.com/site/test',
        expected_domain => 'test-site.sites.google.com', 
    },
    {
        href => 'https://sites.google.com/site/-test1__test2-test3--/test4?param=1',
        expected_domain => 'test1-test2-test3-site.sites.google.com', 
    },
    {
        href => 'https://sites.google.com/site/',
        expected_domain => undef, 
    },
    {
        href => 'https://sites.google.com/view/test123/',
        expected_domain => 'test123-view.sites.google.com', 
    },
    {
        href => 'https://sites.google.com/test1/test2/',
        expected_domain => undef, 
    },

    # enabled_domains
    {
        href => 'https://sites.google.com/site/test',
        expected_domain => undef, 
        enabled_domains => {},
    },
    {
        href => 'https://sites.google.com/site/test',
        expected_domain => 'test-site.sites.google.com', 
        enabled_domains => {'sites.google.com' => 1},
    },
    {
        href => 'https://sites.google.com/site/test',
        expected_domain => undef, 
        enabled_domains => {'ok.ru' => 1},
    },
);

for my $test_case (@test_data) {
    my $href = $test_case->{href};
    my $test_name = 'aggregator_domain for href = ' . (defined($href) ? qq{"$href"} : 'undef');
    my $enabled_domains = $test_case->{enabled_domains} // { map { $_ => 1 } @AggregatorDomains::ALLOWED_DOMAINS };

    my $actual_domain = AggregatorDomains::extract_aggregator_domain_from_url($href, $enabled_domains);
    is($actual_domain, $test_case->{expected_domain}, $test_name);
}


done_testing();
