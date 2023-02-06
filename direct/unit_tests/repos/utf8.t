#!/usr/bin/perl

# проверка на то, что все исходники в utf8

use warnings;
use strict;

use File::Slurp;
use Encode;
use Test::More;

use Test::ListFiles;
use Settings;

my $DIR = $Settings::ROOT;


my $text_files = qr/\.(arcignore|complete|gitignore|pl|pm|psgi|t|wsdl|xsd|xsl|sql|json|css|js|tt2|txt|wiki|xml|md|html|bemhtml|svg|svnignore|ometajs|version|ycssjs|sh|blocks\\-levels|rb|mk|phtml|tex|sty|lua|tmpl|borschik|editorconfig|eslintignore|eslintrc|jshintignore|jshintrc|npmrc|styl|ru|man|text|migr|data|yaml|conf|cfg|ctmpl|test|token|yml|cnf|exclude|pot|po|d|dirs|postinst|prerm|postrm|preinst|py|make|php|sug|trans|makefile)$/;


my @files = sort
#    grep {($svn_props->{$_}->{'svn:mime-type'}||'') !~ /application|image/}
#    grep { $ft->checktype_filename($_) !~ /image/ }
    grep { $_ =~ /$text_files/  }
    grep { !/stopword.lst/ }
    grep { !/advq_regions_[a-z]{2}\.html/ }
    grep { -f }
    Test::ListFiles->list_repository($DIR);

Test::More::plan(tests => scalar(@files));

for my $file (@files) {
    my $tt_text = read_file($file);
    
    my ($bad_line_r, $bad_line_utf8);
    my @lines = split /\n/, $tt_text;
    for my $i (0..$#lines) {
        if (!defined $bad_line_r && $lines[$i] =~ /\r/) {
            $bad_line_r = $i+1;
        }
        if (!defined $bad_line_utf8) {
            eval {Encode::decode("utf-8", $lines[$i], Encode::FB_CROAK);};
            $bad_line_utf8 = $i+1 if $@;
        }
    }

    #ok(!defined $bad_line_r, "\\r in $file".(defined $bad_line_r ? " (line: $bad_line_r)" : ''));
    ok(!defined $bad_line_utf8, "utf8 in $file".(defined $bad_line_utf8 ? " (line: $bad_line_utf8)" : ''));
}

