#!/usr/bin/perl

=head2 DESCRIPTION

    Проверяет все не-бинарные файлы на отсутствие следов недоразрешённых SVN-конфликтов.
    Ловит строки вида:
        <<<<<<< .working
        >>>>>>> .merge-right.rNNNNN

=cut

use strict;
use warnings;

use Yandex::Test::UTF8Builder;
use Test::More;

use File::Basename qw/basename/;
use File::Slurp;

use Test::ListFiles;
use Settings;

use utf8;

my $text_files = qr/\.(arcignore|complete|gitignore|pl|pm|psgi|t|wsdl|xsd|xsl|sql|json|css|js|tt2|txt|wiki|xml|md|html|bemhtml|svg|svnignore|ometajs|version|ycssjs|sh|blocks\\-levels|rb|mk|phtml|tex|sty|lua|tmpl|borschik|editorconfig|eslintignore|eslintrc|jshintignore|jshintrc|npmrc|styl|ru|man|text|migr|data|yaml|conf|cfg|ctmpl|test|token|yml|cnf|exclude|pot|po|d|dirs|postinst|prerm|postrm|preinst|py|make|php|sug|trans|makefile)$/;

my $ROOT = $Settings::ROOT;
my $script_name = basename($0);

my @files_to_check = grep {!/\Q$script_name\E/ && $_ =~ /$text_files/  && -f} Test::ListFiles->list_repository($ROOT);

Test::More::plan(tests => scalar(@files_to_check));

for my $file (@files_to_check) {
    my $text = read_file($file, binmode => ':utf8');
    ok($text !~ /^(?:<<<<<<< \.working|>>>>>>> \.merge-)/m, "в файле $file нет следов недоразрешённых SVN-конфликтов");
}
