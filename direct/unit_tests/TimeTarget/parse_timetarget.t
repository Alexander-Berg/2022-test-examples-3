#!/usr/bin/perl

# $Id$

use warnings;
use strict;
use Test::More;
use Test::Deep;
use Yandex::Test::UTF8Builder;

BEGIN { use_ok( 'TimeTarget' ); }

use utf8;
use open ':std' => ':utf8';

*ptt = \&TimeTarget::parse_timetarget;

cmp_deeply(ptt(''), {time_target_preset=>'all',
                       timeTarget => $TimeTarget::DEFAULT_TIMETARGET,
                       timeTargetMode => 'simple',
                       show_on_weekend => 1,
                       time_target_working_holiday => 0
           });

cmp_deeply(ptt(undef), {time_target_preset=>'all',
                       timeTarget => $TimeTarget::DEFAULT_TIMETARGET,
                       timeTargetMode => 'simple',
                       show_on_weekend => 1,
                       time_target_working_holiday => 0
           });

cmp_deeply(ptt('1A'), {time_target_preset=>'worktime',
                       timeTarget => '1A',
                       timeTargetMode => 'simple',
                       show_on_weekend => '',
                       time_target_working_holiday => 0
           });

cmp_deeply(ptt('2BCD3BCD4BCD'), {time_target_preset=>'worktime',
                       timeTarget => '2BCD3BCD4BCD',
                       timeTargetMode => 'simple',
                       show_on_weekend => '',
                       time_target_working_holiday => 0
           });

cmp_deeply(ptt('1A2B'), {time_target_preset=>'other',
                       timeTarget => '1A2B',
                       timeTargetMode => 'simple',
                       show_on_weekend => '',
                       time_target_working_holiday => 0
           });


cmp_deeply(ptt('1A2A3A8DE'), {time_target_preset=>'worktime',
                       time_target_holiday => 1, time_target_holiday_from => 3, time_target_holiday_to => 5,
                       timeTarget => '1A2A3A',
                       timeTargetMode => 'simple',
                       show_on_weekend => '',
                       time_target_working_holiday => 0
           });

cmp_deeply(ptt('1A2A3A8'), {time_target_preset=>'worktime',
                       time_target_holiday => 1, time_target_holiday_dont_show => 1,
                       timeTarget => '1A2A3A',
                       timeTargetMode => 'simple',
                       show_on_weekend => '',
                       time_target_working_holiday => 0
           });

cmp_deeply(ptt('1AC2AC3AC'), {time_target_preset=>'other',
                       timeTarget => '1AC2AC3AC',
                       timeTargetMode => 'simple',
                       show_on_weekend => '',
                       time_target_working_holiday => 0
           });
cmp_deeply(ptt('1ABC3ABC'), {time_target_preset=>'other',
                       timeTarget => '1ABC3ABC',
                       timeTargetMode => 'simple',
                       show_on_weekend => '',
                       time_target_working_holiday => 0
           });

# with coef
cmp_deeply(ptt('2BbCbD3BCD4BCD'), {
                       time_target_preset => 'worktime',
                       timeTarget => '2BbCbD3BCD4BCD',
                       timeTargetMode => 'extend',
                       show_on_weekend => '',
                       time_target_working_holiday => 0
           });

cmp_deeply(ptt('1A2Bj'), {
                      time_target_preset => 'other',
                      timeTarget => '1A2Bj',
                      timeTargetMode => 'extend',
                      show_on_weekend => '',
                      time_target_working_holiday => 0
          });

# with holiday and coef
cmp_deeply(ptt('1A2A3A8DbEb'), {time_target_preset => 'worktime',
                     time_target_holiday => 1, time_target_holiday_from => 3, time_target_holiday_to => 5, time_target_holiday_coef => 10,
                     timeTarget => '1A2A3A',
                     timeTargetMode => 'extend',
                     show_on_weekend => '',
                     time_target_working_holiday => 0
         });

# пока нет валидации, что коэффициент у праздников должен быть одинаковым на все часы, в parse_timetarget() выбираем из последнего часа
cmp_deeply(ptt('1A2A3A8DuEu'), {time_target_preset => 'worktime',
                     time_target_holiday => 1, time_target_holiday_from => 3, time_target_holiday_to => 5, time_target_holiday_coef => 200,
                     timeTarget => '1A2A3A',
                     timeTargetMode => 'extend',
                     show_on_weekend => '',
                     time_target_working_holiday => 0
         });

cmp_deeply(ptt('1Ab2Ab3A8'), {time_target_preset => 'worktime',
                     time_target_holiday => 1, time_target_holiday_dont_show => 1,
                     timeTarget => '1Ab2Ab3A',
                     timeTargetMode => 'extend',
                     show_on_weekend => '',
                     time_target_working_holiday => 0
         });

cmp_deeply(ptt('1Al2Au3A8'), {time_target_preset => 'worktime',
                     time_target_holiday => 1, time_target_holiday_dont_show => 1,
                     timeTarget => '1Al2Au3A',
                     timeTargetMode => 'extend',
                     show_on_weekend => '',
                     time_target_working_holiday => 0
         });
         
# working holiday
cmp_deeply(ptt('1A2Bj9'), {
                      time_target_preset => 'other',
                      timeTarget => '1A2Bj',
                      timeTargetMode => 'extend',
                      show_on_weekend => '',
                      time_target_working_holiday => 1
          });
         
cmp_deeply(ptt('2BbCbD3BCD6A7BC9'), {
                      time_target_preset => 'other',
                      timeTarget => '2BbCbD3BCD6A7BC',
                      timeTargetMode => 'extend',
                      show_on_weekend => 1,
                      time_target_working_holiday => 1
          });
         
cmp_deeply(ptt('1DE2ABbCbD3BCD7BC9'), {
                      time_target_preset => 'other',
                      timeTarget => '1DE2ABbCbD3BCD7BC',
                      timeTargetMode => 'extend',
                      show_on_weekend => '',
                      time_target_working_holiday => 1
          });

# string with setting `time_target_preset`: all
cmp_deeply(ptt(';p:a'), {
                      time_target_preset => 'all',
                      timeTarget => $TimeTarget::DEFAULT_TIMETARGET,
                      timeTargetMode => 'simple',
                      show_on_weekend => 1,
                      time_target_working_holiday => 0

          });

# string with setting `time_target_preset`: other
cmp_deeply(ptt('1DE2ABbCbD3BCD7BC9;p:o'), {
                      time_target_preset => 'other',
                      timeTarget => '1DE2ABbCbD3BCD7BC',
                      timeTargetMode => 'extend',
                      show_on_weekend => '',
                      time_target_working_holiday => 1
          });

# string with setting `time_target_preset`: worktime
cmp_deeply(ptt('1Ab2Ab3A8;p:w'), {
                      time_target_preset => 'worktime',
                      time_target_holiday => 1, time_target_holiday_dont_show => 1,
                      timeTarget => '1Ab2Ab3A',
                      timeTargetMode => 'extend',
                      show_on_weekend => '',
                      time_target_working_holiday => 0
          });

# old format
cmp_deeply(ptt('123-56-ABC--FGHI--LMNOPQRSTUVWX'), {time_target_preset=>'other',
                       timeTarget => '4DEJK7DEJK',
                       timeTargetMode => 'simple',
                       show_on_weekend => '',
                       time_target_working_holiday => 0
           }, 'old tt format');

cmp_deeply(ptt('12--567ABC----HIJKLMNOPQRSTUVWX'), {time_target_preset=>'worktime',
                       timeTarget => '3DEFG4DEFG',
                       timeTargetMode => 'simple',
                       show_on_weekend => '',
                       time_target_working_holiday => 0
           }, 'old tt format - worktime');

done_testing();

__DATA__
cmp_deeply(ptt('1A'), {time_target_preset=>'',
                       time_target_holiday => 1, time_target_holiday_dont_show => 0, time_target_holiday_from => 0, time_target_holiday_to => 1,
                       timeTarget => '',
                       show_on_weekend => '',
                       time_target_working_holiday => 0
                       timeTargetMode => 'simple', # or 'extend'
           });
