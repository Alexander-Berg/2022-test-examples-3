#!/usr/bin/perl

use Direct::Modern;

use Test::MockObject::Extends;
use Test::More;

use BS::URL;
use Property;

my %DATA = (
    test => { url_path => '/test/path', host_type => 'testtype', has_preprod => 1 },
);
my %TYPE2HOST = (
    testtype => { host => 'testhost', pre => 'testpre' },
);
my $PROP_PCT = 50;
my $prop = Test::MockObject::Extends->new( Property->new(BS::URL::EXPORT_PREPROD_USAGE_PCT_PROP_NAME) );
$prop->mock('get', sub { return $PROP_PCT });

*BS::URL::DATA = \%DATA;
*BS::URL::TYPE2HOST = \%TYPE2HOST;
{
    no warnings 'redefine';
    *Property::new = sub { return $prop };
}

my $default_url = 'http://testhost/test/path';
my $pre_url = 'http://testpre/test/path';

is(BS::URL::get('test'), $default_url, 'default host in url without params');
is(BS::URL::get('test', ClientID => 99), $default_url, 'default host in url for ClientID % 100 > PROPERTY_PERCENT');
is(BS::URL::get('test', ClientID => $PROP_PCT), $default_url, 'default host in url for ClientID % 100 == PROPERTY_PERCENT');
is(BS::URL::get('test', ClientID => 1), $pre_url, 'pre-host in url for ClientID % 100 < PROPERTY_PERCENT');
is(BS::URL::get('test', ClientID => 100), $pre_url, 'pre-host in url for ClientID % 100 = 0 < PROPERTY_PERCENT');

done_testing();
