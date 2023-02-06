package Perl::Critic::Policy::ControlStructures::ProhibitAssignmentInConditions;

=head2 

    Проверка для perlcritic, запрещающая использовать присваивание внутри условий,
    если переменная не объявлена здесь же

=cut

use strict;
use warnings;

use Readonly;

use Perl::Critic::Utils qw{ :severities :classification hashify };
use base 'Perl::Critic::Policy';

sub supported_parameters { return ()                  }
sub default_severity     { return $SEVERITY_HIGH      }
sub default_themes       { return qw( core bugs ) }
sub applies_to           { return 'PPI::Token::Word'  }

Readonly::Hash my %CHECK_TOKENS => hashify(qw/if elsif unless while until/);
Readonly::Hash my %DECLARE_TOKENS => hashify(qw/my state local our/);

sub check_expression {
    my ($self, $elem) = @_;

    for my $children ($elem->schildren) {
        if ($children->isa('PPI::Statement::Expression')) {
            my $violation = $self->check_expression($children);
            if ($violation) {
                return $violation;
            } else {
                next;
            }
        } else {
            next if ! is_assignment_operator($children);
            my $var = $children->sprevious_sibling;
            next if ! ($var->isa('PPI::Token::Symbol') || $var->isa('PPI::Structure::List'));

            my $declare_token = $var->sprevious_sibling;

            next if $declare_token && $declare_token->isa('PPI::Token::Word') && $DECLARE_TOKENS{ $declare_token->content };

            return $self->violation('External variable assigned in conditional statement', 'Declare variable inside of condition or remove the assignment operator', $children);
            
        }
    }
    return;
}

sub violates {
    my ( $self, $token, undef ) = @_;

    return if ! $CHECK_TOKENS{ $token->content };

    my $condition = $token->snext_sibling;
    return if ! $condition;
    return if ! $condition->isa('PPI::Structure::Condition');

    my $block = $condition->snext_sibling;

    return if ! $block;
    return if ! $block->isa('PPI::Structure::Block');

    return $self->check_expression($condition);
}

1;
