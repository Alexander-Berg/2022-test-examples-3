#!/usr/bin/perl

use my_inc '..';

=head1 DEPLOY

# approved by pankovpv
# .migr
{
  type => 'script',
  when => 'after',
  time_estimate => "меньше секунды",
  comment => "на разработческих и тестовых средах деплой может не сработать
    из-за отсутствия в БД специально созданной тестовой кампании - это некритично
    к запуску в продакшене"
}

=cut

use warnings;
use strict;
use utf8;

use Yandex::DBTools;

use Settings;
use ScriptHelper;

use geo_regions ();
use FakeAdminTools ();

$log->out('START');

my $new_pid = eval {
    FakeAdminTools::create_dynamic_adgroup(
        cid => 12950249,
        uid => 36814023, # elvira-client
        domain => 'www.mvideo.ru',
        banner_bodies => [
            'Бытовая техника в М-Видео'
        ],
        geo => $geo_regions::MOSCOW,
        dynamic => [
            {
                condition => [
                    { type => 'any' },
                ],
                price => 0.01,
                price_context => 0.01,
            },
        ],
    );
};

$log->out({new_pid => $new_pid, '$@' => $@});

$log->out('FINISH');

