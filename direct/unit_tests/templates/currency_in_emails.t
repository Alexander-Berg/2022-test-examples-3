#!/usr/bin/perl

=head1 DESCRIPTION

Проверяем, что шаблоны писем всецело поддерживают единовалютность

=cut

use warnings;
use strict;

use File::Slurp;
use Test::More;

use Test::ListFiles;
use Settings;
use Yandex::Test::UTF8Builder;
use Yandex::I18n;

use utf8;
use open ':std' => ':utf8';

my @SKIP = qw(
    force_currency_convert
    force_currency_convert_easy_user
);
my $SKIP_RE = '/(?:'.join('|', @SKIP).')$';

my $dir = $Yandex::MailTemplate::EMAIL_TEMPLATES_FOLDER.'/'.$Yandex::I18n::DEFAULT_LANG;
my @files = grep {!/$SKIP_RE/} grep {-f $_} Test::ListFiles->list_repository($dir);
Test::More::plan(tests => scalar @files);

for my $file (sort @files) {
    my $template = read_file($file, binmode => ':utf8');
    my $result = 1;
    # у.е. по-русски и латинницей
    if ($template =~ /[уy]\.\s*[еe]\./) {
        $result = 0;
    }

    ok($result, $file);
}
