#!/usr/bin/env perl

=head1 DESCRIPTION

Проверяем, что шаблоны СМС содержат логины только в переменной client_login и список таких шаблонов известен

=cut

use Direct::Modern;

use Path::Tiny;
use Test::More;
use Test::ListFiles;

use Settings;

use Yandex::I18n;

my %ALLOWED = map {$_ => undef} qw(
    active_orders_money_out_campaign_stopped_sms
);

my $dir = $Yandex::MailTemplate::EMAIL_TEMPLATES_FOLDER.'/'.$Yandex::I18n::DEFAULT_LANG;
my @files = grep {/_sms$/} grep {-f $_} Test::ListFiles->list_repository($dir);
Test::More::plan(tests => scalar @files);

for my $file (sort @files) {
    my $template = path($file)->slurp_utf8;
    my $name = path($file)->basename;
    my $result = 1;
    # переменная с логином, но не sms_client_login
    if ($template =~ /(?<!sms_client_)login/) {
        $result = 0;
    }
    if ($template =~ /sms_client_login/ && !exists($ALLOWED{$name})) {
        $result = 0;
    }

    ok($result, $file);
}
