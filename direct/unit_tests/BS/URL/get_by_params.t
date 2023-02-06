#!/usr/bin/perl

use Direct::Modern;

use Test::More;

use BS::URL;

my %DATA = (
    test => { url_path => '/test/path', host_type => 'testtype', has_preprod => 1 },
);
my %TYPE2HOST = (
    testtype => { host => 'testhost', pre => 'testpre' },
);
my $MYHOST = 'myhost';

*BS::URL::DATA = \%DATA;
*BS::URL::TYPE2HOST = \%TYPE2HOST;

my $default_url = 'http://testhost/test/path';
my $pre_url = 'http://testpre/test/path';
my $my_url = 'http://myhost/test/path';

is(BS::URL::get_by_params('test'), $default_url, 'default host in url without params');
is(BS::URL::get_by_params('test', want_preprod => 0), $default_url, 'default host in url for want_preprod = 0');
is(BS::URL::get_by_params('test', want_preprod => 1), $pre_url, 'pre-host in url for want_preprod = 1');
is(BS::URL::get_by_params('test', want_host => $MYHOST), $my_url, 'my-host in url for want_host param');
is(BS::URL::get_by_params('test', want_host => $MYHOST, want_preprod => 1), $my_url, 'my-host in url for want_host param and want_preprod = 1 (want_host has higher priority)');

done_testing();
