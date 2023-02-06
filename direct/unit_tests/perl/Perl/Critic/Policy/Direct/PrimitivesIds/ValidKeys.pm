package Perl::Critic::Policy::Direct::PrimitivesIds::ValidKeys;

=head2 

    Проверка для perlcritic валидности первого параметра, передаваемого в функции PrimitivesIds
    Критерий простой - если есть тест в PrimitivesIds/all.t - значик ключ поддерживается

=cut

use strict;
use warnings;

use feature 'state';
use JSON;
use Yandex::Shell;
use Settings;

use Perl::Critic::Utils qw{ :severities :classification :ppi };
use base 'Perl::Critic::Policy';

sub supported_parameters { return ()                  }
sub default_severity     { return $SEVERITY_HIGH      }
sub default_themes       { return qw( core bugs ) }
sub applies_to           { return 'PPI::Token::Word'  }

sub violates {
    my ( $self, $elem, undef ) = @_;

    my $tests = get_tests();

    return if ! defined $tests->{$elem};
    return if ! is_function_call($elem);

    my $arg = first_arg($elem);
    return if !$arg;
    return if $arg->content =~ /^\$/;
    return if $arg->content =~ /^['"]?(\w+)['"]?$/ && $tests->{$elem}->{$1};
    
    return $self->violation( "Valid set of keys in PrimitivesIds functions", "No test for $elem($arg, ..) in unit_tests/PrimitivesIds/all.t", $elem );
}

sub get_tests {
    state $tests;
    if (!defined $tests) {
       $tests = from_json(yash_qx($^X, "$Settings::ROOT/unit_tests/PrimitivesIds/all.t", "print-tests"));
    }
    return $tests;
}

1;
