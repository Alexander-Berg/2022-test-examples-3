#!/usr/bin/env perl

use Direct::Modern;
use open ':std' => 'utf8';

use Test::More;
use Test::Deep;
use Storable qw/dclone/;

use Settings;

use Yandex::TimeCommon qw/tomorrow today yesterday mysql_round_day/;
use Yandex::DateTime qw/now/;

use Direct::Test::DBObjects;
use Direct::Strategy;
use Direct::Strategy::AutobudgetMaxImpressionsCustomPeriod;
use Direct::Validation::Errors;
use CampaignTools;

BEGIN {
    use_ok 'Direct::Validation::Strategy';
}

sub validate_start {
    my @params = @_;
    Direct::Validation::Strategy::validate_start(@params);
}
sub validate_finish {
    my @params = @_;
    Direct::Validation::Strategy::validate_finish(@params);
}
sub validate_budget {
    my @params = @_;
    Direct::Validation::Strategy::validate_budget(@params);
}

sub validate_crr {
    my @params = @_;
    Direct::Validation::Strategy::validate_crr(@params);
}

sub min_available_budget {
    my @params = @_;
    CampaignTools::calc_min_available_budget_for_strategy_with_custom_period(@params);
}

subtest 'validate_crr' => sub {
    Direct::Test::DBObjects->create_tables;
    my $db_objects = Direct::Test::DBObjects->new;
    $db_objects->with_campaign( 'text' );
    my $campaign = $db_objects->campaign;
    my $strategy = Direct::Strategy::AutobudgetCrr->new(
        name   => 'autobudget_crr_test',
        start  => mysql_round_day(today(), delim => '-'),
        finish => mysql_round_day(tomorrow(), delim => '-'),
        sum    => 5000,
    );
    
    subtest 'ok' => sub {
        is( validate_crr(100, $campaign, $strategy), undef );
    };

    subtest 'ok min' => sub {
        is( validate_crr(1, $campaign, $strategy, required => 1), undef );
    };

    subtest 'ok max' => sub {
        is( validate_crr(500, $campaign, $strategy, required => 1), undef );
    };

    subtest 'unspecified field' => sub {
        cmp_deeply(
            validate_crr(undef, $campaign, $strategy, required => 1),
            error_EmptyField('Не указано значение доли рекламных расходов (должно быть от 1% до 500%)')
        );
    };
    
    subtest 'less than min' => sub {
        cmp_deeply(
            validate_crr(0, $campaign, $strategy, required => 1),
            error_InvalidField('Значение доли рекламных расходов должно быть не меньше 1%')
        );
    };

    subtest 'greater than max' => sub {
        cmp_deeply(
            validate_crr(501, $campaign, $strategy, required => 1),
            error_InvalidField('Значение доли рекламных расходов должно быть не больше 500%')
        );
    };

    subtest 'invalid format' => sub {
        cmp_deeply(
            validate_crr(103.2, $campaign, $strategy, required => 1),
            error_InvalidFormat('Значение доли рекламных расходов должно быть целым числом')
        );
    };
};

subtest 'validate_start' => sub {
        Direct::Test::DBObjects->create_tables;
        my $db_objects = Direct::Test::DBObjects->new;
        $db_objects->with_campaign( 'cpm_banner', {start_date => "0000-00-00", finish_date => "0000-00-00"} );
        my $campaign = $db_objects->campaign;

        my $strategy = Direct::Strategy::AutobudgetMaxImpressionsCustomPeriod->new(
                    budget => 1000, 
                    name => "autobudget_max_reach_custom_period", 
                    avg_cpm => 100,
                    start => "0000-00-00", 
                    finish => "0000-00-00",
                    version => 1
                );

        subtest 'ok' => sub {
            my $start = mysql_round_day(tomorrow(), delim => '-');
            $strategy->start($start);
            $campaign->_strategy(dclone($strategy));
            is( validate_start( $start, $campaign, $strategy ), undef );
        };

        subtest 'undef field' => sub {
            my $start = undef;
            cmp_deeply( 
                validate_finish( $start, $campaign, $strategy ), 
                error_ReqField()
            );
        };
        
        subtest 'empty field' => sub {
            my $start = "   ";
            cmp_deeply( 
                validate_start( $start, $campaign, $strategy ), 
                error_EmptyField("Дата начала периода не задана")
            );
        };

        subtest 'invalid format' => sub {
            my $start = "qweqwe";
            cmp_deeply( 
                validate_start( $start, $campaign, $strategy ), 
                error_InvalidFormat_IncorrectDate()
            );
        };

        subtest 'strategy start earlier than campaign start' => sub {
            my $start = mysql_round_day(today(), delim => '-');
            my $campaign_start = mysql_round_day(tomorrow(), delim => '-');
            $strategy->start($start);
            $campaign->start_date($campaign_start);
            cmp_deeply( 
                validate_start( $start, $campaign, $strategy ), 
                 error_InconsistentState("Дата начала периода стратегии не может быть меньше даты начала кампании")
            );
        };

        subtest 'strategy start earlier than today' => sub {
            my $start = mysql_round_day(yesterday(), delim => '-');
            $strategy->start($start);
            cmp_deeply( 
                validate_start( $start, $campaign, $strategy ), 
                error_InvalidFormat("Значение даты в поле #field# не может быть меньше текущей даты")
            );
        };

        subtest 'strategy start earlier than today, strategy not change' => sub {
            my $start = "2017-11-11";
            $strategy = Direct::Strategy::AutobudgetMaxImpressionsCustomPeriod->new(
                budget => 1000, 
                name => "autobudget_max_reach_custom_period", 
                avg_cpm => 100,
                start => $start, 
                finish => "2017-11-15",
                version => 1
            );
            $campaign->_strategy(dclone($strategy));
            $campaign->start_date('2017-11-10');
            is( validate_start( $start, $campaign, $strategy ), undef );
        };

    };

    subtest 'validate_finish' => sub {
        Direct::Test::DBObjects->create_tables;
        my $db_objects = Direct::Test::DBObjects->new;
        $db_objects->with_campaign( 'cpm_banner', {start_date => "0000-00-00", finish_date => "0000-00-00"} );
        my $campaign = $db_objects->campaign;

        my $strategy = Direct::Strategy::AutobudgetMaxImpressionsCustomPeriod->new(
                    budget => 1000, 
                    name => "autobudget_max_reach_custom_period", 
                    avg_cpm => 100,
                    start => "0000-00-00", 
                    finish => "0000-00-00",
                    version => 1
                );

        subtest 'ok' => sub {
            my $start = mysql_round_day(today(), delim => '-');
            my $finish = mysql_round_day(tomorrow(tomorrow()), delim => '-');
            $strategy->finish($finish);
            $strategy->start($start);
            $campaign->_strategy(dclone($strategy));
            is( validate_finish( $finish, $campaign, $strategy ), undef );
        };

        subtest 'undef field' => sub {
            my $finish = undef;
            cmp_deeply( 
                validate_finish( $finish, $campaign, $strategy ), 
                error_ReqField()
            );
        };
        
        subtest 'empty field' => sub {
            my $finish = "   ";
            cmp_deeply( 
                validate_finish( $finish, $campaign, $strategy ), 
                error_EmptyField("Дата окончания периода не задана")
            );
        };

        subtest 'invalid format' => sub {
            my $finish = "qweqwe";
            cmp_deeply( 
                validate_finish( $finish, $campaign, $strategy ), 
                error_InvalidFormat_IncorrectDate()
            );
        };

        subtest 'strategy finish later than campaign finish' => sub {
            my $finish = mysql_round_day(tomorrow(tomorrow()), delim => '-');
            my $campaign_finish = mysql_round_day(today(), delim => '-');
            $strategy->finish($finish);
            $campaign->finish_date($campaign_finish);
            cmp_deeply( 
                validate_finish( $finish, $campaign, $strategy ), 
                 error_InconsistentState("Дата окончания периода стратегии не может быть больше даты окончания кампании")
            );
        };

        subtest 'strategy finish earlier than strategy start' => sub {
            my $finish = mysql_round_day(yesterday(), delim => '-');
            my $start = mysql_round_day(today(), delim => '-');
            $strategy->finish($finish);
            $strategy->start($start);
            cmp_deeply( 
                validate_finish( $finish, $campaign, $strategy ), 
                error_InconsistentState('Значение даты в поле #from# не может быть больше значения даты в поле #to#')
            );
        };

        subtest 'strategy finish earlier than strategy start, strategy start = 0000-00-00' => sub {
            my $finish = mysql_round_day(yesterday(), delim => '-');
            $strategy->finish($finish);
            $strategy->start('0000-00-00');
            ok( !validate_finish( $finish, $campaign, $strategy ) );
        };


        subtest 'strategy finish earlier than today' => sub {
            my $finish = mysql_round_day(yesterday(), delim => '-');
            my $start = mysql_round_day(yesterday(yesterday()), delim => '-');
            $strategy->finish($finish);
            $strategy->start($start);
            cmp_deeply( 
                validate_finish( $finish, $campaign, $strategy ), 
                error_InvalidFormat("Значение даты в поле #field# не может быть меньше либо равным текущей дате")
            );
            
        };

        subtest 'strategy finish earlier than today, strategy not change' => sub {
            my $finish = "2017-11-15";
            $strategy = Direct::Strategy::AutobudgetMaxImpressionsCustomPeriod->new(
                budget => 1000, 
                name => "autobudget_max_reach_custom_period", 
                avg_cpm => 100,
                start => "2017-11-11", 
                finish => $finish,
                version => 1
            );
            $campaign->_strategy(dclone($strategy));
            $campaign->start_date('2017-11-10');
            $campaign->finish_date('2017-11-16');
            is( validate_start( $finish, $campaign, $strategy ), undef );
        };

        subtest 'strategy finish earlier than today, auto_prolongation change' => sub {
            my $finish = "2017-11-15";
            $strategy = Direct::Strategy::AutobudgetMaxImpressionsCustomPeriod->new(
                budget => 1000, 
                name => "autobudget_max_reach_custom_period", 
                avg_cpm => 100,
                start => "2017-11-11", 
                finish => $finish,
                auto_prolongation => 0,
                version => 1
            );
            $campaign->_strategy(dclone($strategy));
            $strategy->auto_prolongation(1);
            $campaign->start_date('2017-11-10');
            $campaign->finish_date('2017-11-16');
            is( validate_start( $finish, $campaign, $strategy ), undef );
        };

        subtest 'period is 1 day' => sub {
            my $finish = mysql_round_day(today(), delim => '-');
            my $start = mysql_round_day(today(), delim => '-');
            $strategy->finish($finish);
            $strategy->start($start);
            cmp_deeply( 
                validate_finish( $finish, $campaign, $strategy ), 
                error_InvalidFormat('Для стратегий с произвольным периодом, период не может быть меньше 1 дня')
            );
        };

    };

    subtest 'validate_budget' => sub {
        Direct::Test::DBObjects->create_tables;
        my $db_objects = Direct::Test::DBObjects->new;
        $db_objects->with_campaign( 'cpm_banner' );
        my $campaign = $db_objects->campaign;

        my $strategy = Direct::Strategy::AutobudgetMaxImpressionsCustomPeriod->new(
                    budget => 1000, 
                    name => "autobudget_max_reach_custom_period", 
                    avg_cpm => 100,
                    start => mysql_round_day(today(), delim => '-'),
                    finish => mysql_round_day(tomorrow(), delim => '-'),
                    version => 1
                );

        subtest 'ok max' => sub {
            is( validate_budget( 200_000_000, $campaign, $strategy ), undef );
        };

        subtest 'ok min' => sub {
            is( validate_budget( 600, $campaign, $strategy ), undef );
        };

        subtest 'undef field' => sub {
            cmp_deeply( 
                validate_budget( undef, $campaign, $strategy, required => 1 ), 
                error_EmptyField('Не указана сумма бюджета на фиксированный период.')
            );
        };

        subtest 'invalid format' => sub {
            cmp_deeply( 
                validate_budget( 'qwe', $campaign, $strategy ),
                error_InvalidFormat('Неверно указан бюджет на фиксированный период.')
            );
        };

        subtest 'budget less then min' => sub {
            cmp_deeply( 
                validate_budget( 599, $campaign, $strategy),
                error_InvalidField('Бюджет на фиксированный период не может быть меньше 600.00 руб.')
            );
        };

        subtest 'budget greater then max' => sub {
            cmp_deeply( 
                validate_budget( 700_000_001, $campaign, $strategy),
                error_InvalidField('Бюджет на фиксированный период не может быть больше 200 000 000.00 руб.')
            );
        };

    };

    subtest 'validate_budget_when_edit_period_strategy' => sub {
        # Проверка редактирования бюджета в течение периода. Работает за фичей has_edit_avg_cpm_without_restart_enabled
        # https://st.yandex-team.ru/DIRECT-106529

        Direct::Test::DBObjects->create_tables;
        my $db_objects = Direct::Test::DBObjects->new;
        $db_objects->with_campaign( 'cpm_banner' );
        my $campaign = $db_objects->campaign;

        # Параметры стратегии для валидации бюджета при редактировании
        # всего дней в периоде ($days_count_in_period): 10
        # сколько прошло дней от начала периода ($days_count_till_now): 1
        # осталось дней до конца периода ($days_remain): 9
        # минимально допустимый бюджет для текущих параметров (min_budget) =
        # = (10000 / $days_count_in_period * $days_count_till_now) + 300 * $days_remain = 1000 + 300 * 9 = 3700
        my $budget = 10000;
        my $strategy = Direct::Strategy::AutobudgetMaxImpressionsCustomPeriod->new(
            budget => $budget,
            name => "autobudget_max_impressions_custom_period",
            avg_cpm => 100,
            start => mysql_round_day(today(), delim => '-'),
            finish => now()->add(days => 9)->ymd(),
            version => 1
        );

        $campaign->_strategy($strategy);

        subtest 'increase budget' => sub {
            is(validate_budget( 10100, $campaign, $strategy, has_edit_avg_cpm_without_restart_enabled => 1), undef);
        };

        subtest 'decrease budget; less then available minimum' => sub {

            my $new_budget = 1000;
            my $new_strategy = dclone($strategy);
            $new_strategy->budget($new_budget);

            cmp_deeply(
                validate_budget( $new_budget, $campaign, $new_strategy, has_edit_avg_cpm_without_restart_enabled => 1),
                error_InvalidField('Бюджет на текущий период не может быть меньше 3 700.00 руб.')
            );
        };

        subtest 'decrease budget; greater then available minimum' => sub {

            my $new_budget = 4000;
            my $new_strategy = dclone($strategy);
            $new_strategy->budget($new_budget);

            is(validate_budget( $new_budget, $campaign, $new_strategy, has_edit_avg_cpm_without_restart_enabled => 1), undef);
        };

        subtest 'extend finish time; budget equals available minimum' => sub {

            #увеличиваем период до 30 дней так, что минимальный бюджет = 1000 + 300 * 30 = 10000
            my $new_finish_time = now()->add(days => 30)->ymd();
            my $new_strategy = dclone($strategy);
            $new_strategy->finish($new_finish_time);

            is(validate_budget( $budget, $campaign, $new_strategy, has_edit_avg_cpm_without_restart_enabled => 1), undef);
        };

        subtest 'extend finish time; budget less then available minimum' => sub {

            #увеличиваем период до 31 дней так, что минимальный бюджет = 1000 + 300 * 31 = 10300
            my $new_finish_time = now()->add(days => 31)->ymd();
            my $new_strategy = dclone($strategy);
            $new_strategy->finish($new_finish_time);
            cmp_deeply(
                validate_budget( $budget, $campaign, $new_strategy, has_edit_avg_cpm_without_restart_enabled => 1),
                error_InvalidField('Бюджет на текущий период не может быть меньше 10 300.00 руб.')
            );
        };

        subtest 'reduce finish time and budget; budget less then available minimum' => sub {

            #уменьшаем  период до 5 дней так, что минимальный бюджет = 1000 * 1 + 300 * 4 = 2200
            my $new_budget = 2000;
            my $new_finish_time = now()->add(days => 4)->ymd();
            my $new_strategy = dclone($strategy);
            $new_strategy->finish($new_finish_time);
            cmp_deeply(
                validate_budget( $new_budget, $campaign, $new_strategy, has_edit_avg_cpm_without_restart_enabled => 1),
                error_InvalidField('Бюджет на текущий период не может быть меньше 2 200.00 руб.')
            );
        };

        subtest 'reduce finish time and budget; budget greater then available minimum' => sub {

            #уменьшаем  период до 5 дней так, что минимальный бюджет = 1000 * 1 + 300 * 4 = 2200
            my $new_budget = 5000;
            my $new_finish_time = now()->add(days => 4)->ymd();
            my $new_strategy = dclone($strategy);
            $new_strategy->finish($new_finish_time);

            is(validate_budget( $new_budget, $campaign, $new_strategy, has_edit_avg_cpm_without_restart_enabled => 1), undef);
        };
    };

    subtest 'validate_budget_when_edit_period_strategy_at_last_day' => sub {
        Direct::Test::DBObjects->create_tables;
        my $db_objects = Direct::Test::DBObjects->new;
        $db_objects->with_campaign( 'cpm_banner', {start_date => "0000-00-00", finish_date => "0000-00-00"} );
        my $campaign = $db_objects->campaign;

        # Параметры стратегии для валидации бюджета при редактировании
        # всего дней в периоде ($days_count_in_period): 10
        # сколько прошло дней от начала периода ($days_count_till_now): 9
        # осталось дней до конца периода ($days_remain): 1
        # минимально допустимый бюджет для текущих параметров (min_budget) =
        # = (10000 / $days_count_in_period * $days_count_till_now) + 300 * $days_remain = 10000 * 9 + 300 * 1 = 9300
        my $budget = 10000;
        my $start = now()->subtract(days => 8)->ymd();
        my $finish = now()->add(days => 1)->ymd();
        my $strategy = Direct::Strategy::AutobudgetMaxImpressionsCustomPeriod->new(
            budget => $budget,
            name => "autobudget_max_impressions_custom_period",
            avg_cpm => 100,
            start => $start,
            finish => now()->add(days => 1)->ymd(),
            version => 1
        );
        $campaign->start_date($start);
        $campaign->finish_date($finish);
        $campaign->_strategy($strategy);

        subtest 'increase budget' => sub {

            is(validate_budget(10100, $campaign, $strategy, has_edit_avg_cpm_without_restart_enabled => 1), undef);
        };

        subtest 'decrease budget; less then available minimum' => sub {
            my $new_budget = 9000;
            my $new_strategy = dclone($strategy);
            $new_strategy->budget($new_budget);

            cmp_deeply(
                validate_budget($new_budget, $campaign, $new_strategy, has_edit_avg_cpm_without_restart_enabled => 1),
                error_InvalidField('Бюджет на текущий период не может быть меньше 9 300.00 руб.')
            );
        };

        subtest 'decrease budget; greater then available minimum' => sub {
            my $new_budget = 9999;
            my $new_strategy = dclone($strategy);
            $new_strategy->budget($new_budget);

            is(validate_budget($new_budget, $campaign, $new_strategy, has_edit_avg_cpm_without_restart_enabled => 1), undef);
        };

        subtest 'extend finish time; budget less then available minimum' => sub {

            #увеличиваем период до 15 дней так, что минимальный бюджет = 1000 * 9 + 300 * 6 = 10800
            my $new_finish_time = now()->add(days => 6)->ymd();
            $campaign->finish_date($new_finish_time);
            my $new_strategy = dclone($strategy);
            $new_strategy->finish($new_finish_time);
            cmp_deeply(
                validate_budget( $budget, $campaign, $new_strategy, has_edit_avg_cpm_without_restart_enabled => 1),
                error_InvalidField('Бюджет на текущий период не может быть меньше 10 800.00 руб.')
            );
        };
    };

done_testing;
