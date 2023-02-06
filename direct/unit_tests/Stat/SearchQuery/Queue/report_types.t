#!/usr/bin/perl

=encoding utf8

=head1 NAME

    report_types.t

=head1 DESCRIPTION

    Тест проверяет, что все типы отчетов "консистентно" описаны в нескольких настроечных переменных

=cut

use Direct::Modern;

use Test::More;

use Stat::SearchQuery::Queue;

subtest 'check %REPORT_TTL' => sub {
    for my $type (keys %Stat::SearchQuery::Queue::REPORT_TTL) {
        ok(exists $Stat::SearchQuery::Queue::REPORT_TYPES{$type}, "$type is known report type");
    }
};

subtest 'check %REPORT_STATUS_TO_CHECK' => sub {
    for my $type (keys %Stat::SearchQuery::Queue::REPORT_STATUS_TO_CHECK) {
        ok(exists $Stat::SearchQuery::Queue::REPORT_TYPES{$type}, "$type is known report type");
    }
};

subtest 'all types has REPORT_TTL and REPORT_STATUS_TO_CHECK values' => sub {
    for my $type (keys % Stat::SearchQuery::Queue::REPORT_TYPES) {
        ok($Stat::SearchQuery::Queue::REPORT_TTL{$type}, "type $type has 'report ttl' value");
        ok($Stat::SearchQuery::Queue::REPORT_STATUS_TO_CHECK{$type}, "type $type has 'report status to check' value");
    }
};

done_testing();
