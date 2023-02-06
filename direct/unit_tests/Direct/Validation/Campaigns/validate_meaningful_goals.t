#!/usr/bin/env perl

use Direct::Modern;

use base qw/Test::Class/;

use Settings;
use Test::More;
use Yandex::Test::ValidationResult;
use Yandex::Test::UTF8Builder;
use Campaign;

my $FAKE_CID = 999;

sub use_module : Tests( startup => 1 ) {
    use_ok('Direct::Validation::Campaigns');
}


our $goals_request_count;
{
no warnings 'redefine';

*Campaign::Types::get_camp_type = *Campaign::get_camp_type = sub ($$) { 'text' };
*Campaign::get_available_meaningful_goals = sub {
    $goals_request_count ++;
    return +{
        11111 => {},
        22222 => {},
        33333 => {},
    };
};
}


sub empty_goals :Tests(2) {
    local $goals_request_count = 0;
    cmp_validation_result(
        Direct::Validation::Campaigns::validate_meaningful_goals(
            [],
            $FAKE_CID,
        ),
        {},
        'empty list is valid',
    );
    is $goals_request_count, 0, 'no goals request with empty';
}

sub default_goal :Tests(2) {
    local $goals_request_count = 0;
    cmp_validation_result(
        Direct::Validation::Campaigns::validate_meaningful_goals(
            [
                { goal_id => 12, value => 100 },
            ],
            $FAKE_CID,
            currency => 'RUB',
        ),
        {},
        'valid goals',
    );
    is $goals_request_count, 0, 'no goals request with default goal';
}

sub valid_goals :Tests(2) {
    local $goals_request_count = 0;
    cmp_validation_result(
        Direct::Validation::Campaigns::validate_meaningful_goals(
            [
                { goal_id => 11111, value => 200 },
                { goal_id => 22222, value => 100 },
            ],
            $FAKE_CID,
            currency => 'RUB',
        ),
        {},
        'valid goals',
    );
    is $goals_request_count, 1, 'single goals request';
}

sub invalid_goal_id :Tests(1) {
    cmp_validation_result(
        Direct::Validation::Campaigns::validate_meaningful_goals(
            [
                { goal_id => 11111, value => 100 },
                { goal_id => 99999, value => 100 },
            ],
            $FAKE_CID,
            currency => 'RUB',
        ),
        vr_errors(qr/^Цель .* не найдена$/),
        'invalid goal_id',
    );
}

sub invalid_value :Tests(2) {
    cmp_validation_result(
        Direct::Validation::Campaigns::validate_meaningful_goals(
            [
                { goal_id => 11111 },
            ],
            $FAKE_CID,
            currency => 'RUB',
        ),
        vr_errors(qr/^Неверно указана ценность цели .*/),
        'value doesn\'t exist',
    );
    cmp_validation_result(
        Direct::Validation::Campaigns::validate_meaningful_goals(
            [
                { goal_id => 11111, value => "i'm too clever" },
            ],
            $FAKE_CID,
            currency => 'RUB',
        ),
        vr_errors(qr/^Неверно указана ценность цели .*/),
        'invalid value',
    );
}

sub too_few_value :Tests(1) {
    cmp_validation_result(
        Direct::Validation::Campaigns::validate_meaningful_goals(
            [
                { goal_id => 11111, value => 100 },
                { goal_id => 22222, value => 0.01 },
            ],
            $FAKE_CID,
            currency => 'RUB',
        ),
        vr_errors(qr/указана ценность меньше минимальной/),
        'too few goal value',
    );
}

sub too_big_value :Tests(1) {
    cmp_validation_result(
        Direct::Validation::Campaigns::validate_meaningful_goals(
            [
                { goal_id => 11111, value => 100 },
                { goal_id => 22222, value => 100_000_000_000 },
            ],
            $FAKE_CID,
            currency => 'RUB',
        ),
        vr_errors(qr/указана ценность больше максимальной/),
        'too big goal value',
    );
}

sub duplicated_goal :Tests(1) {
    cmp_validation_result(
        Direct::Validation::Campaigns::validate_meaningful_goals(
            [
                { goal_id => 11111, value => 100 },
                { goal_id => 11111, value => 200 },
            ],
            $FAKE_CID,
            currency => 'RUB',
        ),
        vr_errors(qr/указана более одного раза/),
        'duplicated goal',
    );
}

sub valid_goal_and_strategy_use_meaningful_goals :Tests(1) {
    cmp_validation_result(
        Direct::Validation::Campaigns::validate_meaningful_goals(
            [
                { goal_id => 11111, value => 100 },
            ],
            $FAKE_CID,
            currency                              => 'RUB',
            check_availability_to_use_in_strategy => 1,
        ),
        {},
        'set valid goals when strategy use meaningful goals',
    );
}

sub default_goal_but_strategy_use_meaningful_goals :Tests(1) {
    cmp_validation_result(
        Direct::Validation::Campaigns::validate_meaningful_goals(
            [
                { goal_id => 12, value => 100 },
            ],
            $FAKE_CID,
            currency                              => 'RUB',
            check_availability_to_use_in_strategy => 1,
        ),
        vr_errors(qr/Удаление ключевых целей не допускается, поскольку в настройках стратегии выбрана оптимизация по ключевым целям/),
        'set default goals when strategy use meaningful goals',
    );
}

sub empty_goal_but_strategy_use_meaningful_goals :Tests(1) {
    cmp_validation_result(
        Direct::Validation::Campaigns::validate_meaningful_goals(
            [],
            $FAKE_CID,
            currency                              => 'RUB',
            check_availability_to_use_in_strategy => 1,
        ),
        vr_errors(qr/Удаление ключевых целей не допускается, поскольку в настройках стратегии выбрана оптимизация по ключевым целям/),
        'set empty goals when strategy use meaningful goals',
    );
}


__PACKAGE__->runtests();

