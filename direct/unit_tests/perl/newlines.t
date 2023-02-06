#!/usr/bin/perl

=head1 DESCRIPTION

    Проверяет, что у файлов из проверяемых каталогов unix-style переводы строк и есть перевод строки в конце файла.
    Почему:
      * win-концы строк: и так понятно;
      * конечный перевод строки: большинство редакторов, в т.ч. vim с дефолтными настройками его добавляют;
        лучше зафиксировать это соглашение, чтобы не возникало неожиданных модификаций в сценарии "открыл, пересохранил, закрыл"

=cut

use warnings;
use strict;
use File::Slurp;
use Test::More;

use Test::ListFiles;
use Settings;

my @dirs_to_check = (
    "$Settings::ROOT/protected",
    "$Settings::ROOT/perl",
    "$Settings::ROOT/api",
);

# файлы, в которых мы почему-то миримся с непринятыми переводами строк
my %EXCEPT = map {$_ => 1} (
    'api/wsdl/v5/apixslt.xsl',
    'protected/API/Samples/PHP/CreateNewReport.php',
    'protected/API/Samples/PHP/GetReportList.php',
);

my @files_to_check = sort grep {-f} map {Test::ListFiles->list_repository($_)} @dirs_to_check;

for my $file (@files_to_check) {
    my $text = read_file($file);
    $file =~ s!^\Q$Settings::ROOT/\E!!;
    my $win_newline = ($text =~ /\r/);
    my $no_trailing_newline = ($text !~ /\n$/);
    if ( $EXCEPT{$file} ){
        ok( $win_newline || $no_trailing_newline, sprintf "Useless exception(%s): $file", join ",", ($win_newline ? "windows new line style" : ()), $no_trailing_newline ? "no trailing newline" : ());
    } else {
        ok(!$win_newline, "Windows line-endings: $file");
        ok(!$no_trailing_newline, "No newline at end of file: $file");
    }
}

done_testing;
