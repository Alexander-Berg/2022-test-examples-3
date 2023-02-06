#!/usr/bin/perl

    # $Id$

use strict;
use warnings;
use Test::More;
use Test::Deep;

BEGIN { use_ok('DeviceTargeting'); }

my @tests = (
    # [ targeting, is_valid, test_name ]
    [ 'desktop', 0, 'все остальные устройства - старое обозначение' ],
    [ 'other_devices', 1, 'все остальные устройства - новое обозначение' ],
    [ '', 1, 'пустой таргетинг' ],
    [ ',', 0, 'пустой таргетинг (,)' ],
    [ ',,', 0, 'пустой таргетинг (,,)' ],
    [ 'ipad,iphone,android_tablet,android_phone,desktop', 0, 'некоторые устройства' ],
    [ 'ipad,iphone,android_tablet,android_phone,other_devices', 1, 'некоторые устройства' ],
    [ 'ipad,android_phone', 1, 'некоторые устройства' ],
    [ ',ipad,iphone,android_tablet,android_phone,desktop', 0, 'лишний разделитель в начале' ],
    [ ',ipad', 0, 'лишний разделитель в начале' ],
    [ 'ipad,iphone,android_tablet,,android_phone,desktop', 0, 'лишний разделитель в середине' ],
    [ 'ipad,iphone,android_tablet,android_phone,desktop,', 0, 'лишний разделитель в конце' ],
    [ 'ipad,android_phone,ipad', 0, 'дубликаты' ],
    [ 'android,phone', 0, 'неизвестные устройства' ],
    [ 'android_phone iphone', 0, 'недопустимые символы' ],
    [ 'android_phone;   iphone, i_pad', 0, 'недопустимые символы' ],
);

push @tests, [(join ',', keys %DeviceTargeting::DEVICES), 1, 'все устройства'];
push @tests, [(join ' ', keys %DeviceTargeting::DEVICES), 0, 'недопустимый разделитель'];

for my $test (@tests) {
    my ($targeting, $is_valid, $test_name) = @$test;

    is(DeviceTargeting::is_valid_device_targeting($targeting), $is_valid, $test_name);
}


*gtg = \&DeviceTargeting::get_target_devices;

is_deeply([ gtg() ], [ [], 0, ], 'undef param');
is_deeply([ gtg('') ], [ [], 0, ], 'empty string param');

is_deeply([ gtg('other_devices,iphone,android_phone,ipad,android_tablet') ], [ [], 0, ], 'all types devices');
is_deeply([ gtg('other_devices,iphone,android_phone,ipad') ], [ [], 0, ], 'all types devices');
is_deeply([ gtg('other_devices,iphone,ipad') ], [ [], 0, ], 'all types devices');

is_deeply([ gtg('other_devices') ], [ [ 0, ], 0, ], 'only desktop');
is_deeply([ gtg('iphone,android_phone') ], [ [ 1, ], 0, ], 'only phone');
is_deeply([ gtg('ipad,android_tablet') ], [ [ 2, ], 0, ], 'only tablet');

is_deeply([ gtg('other_devices,iphone') ], [ [ 0, 1, ], 0, ], 'desktop and phone');
is_deeply([ gtg('other_devices,android_phone') ], [ [ 0, 1, ], 0, ], 'desktop and phone');
is_deeply([ gtg('other_devices,iphone') ], [ [ 0, 1, ], 0, ], 'desktop and phone');

is_deeply([ gtg('other_devices,ipad') ], [ [ 0, 2, ], 0, ], 'desktop and tablet');
is_deeply([ gtg('other_devices,android_tablet') ], [ [ 0, 2, ], 0, ], 'desktop and tablet');
is_deeply([ gtg('other_devices,ipad,android_tablet') ], [ [ 0, 2, ], 0, ], 'desktop and tablet');

is_deeply([ gtg('iphone,android_tablet') ], [ [ 1, 2, ], 0, ], 'phone and tablet');
is_deeply([ gtg('ipad,android_phone') ], [ [ 1, 2, ], 0, ], 'phone and tablet');

is_deeply([ gtg('android_phone') ], [ [ 1, ], 2, ], 'only Android');
is_deeply([ gtg('android_tablet') ], [ [ 2, ], 2, ], 'only Android');
is_deeply([ gtg('android_tablet,android_phone') ], [ [ 1, 2, ], 2, ], 'only Android');

is_deeply([ gtg('iphone') ], [ [ 1, ], 3, ], 'only iOS');
is_deeply([ gtg('ipad') ], [ [ 2, ], 3, ], 'only iOS');
is_deeply([ gtg('ipad,iphone') ], [ [ 1, 2, ], 3, ], 'only iOS');


*iopdt = \&DeviceTargeting::is_only_phone_device_targeting;

is(iopdt(), 0, 'undef param');
is(iopdt(''), 0, 'empty string param');

is(iopdt('other_devices,iphone,android_phone,ipad,android_tablet'), 0, 'all types devices');
is(iopdt('iphone,ipad'), 0, 'not only phone');
is(iopdt('iphone,android_tablet'), 0, 'not only phone');
is(iopdt('android_tablet'), 0, 'not phone');
is(iopdt('android_tablet,ipad'), 0, 'not phone');
is(iopdt('android_tablet,ipad,other_devices'), 0, 'not phone');
is(iopdt('iphone'), 1, 'only iphone');
is(iopdt('android_phone'), 1, 'only android');
is(iopdt('iphone,android_phone'), 1, 'iphone and android_phone');

done_testing();
