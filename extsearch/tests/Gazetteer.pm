package FastRes::Tests::Gazetteer;

use common::sense;

use FastRes::Tests::Common;
use base qw/FastRes::Tests::Common/;

sub new {
    my ($class, $data) = @_;
    
    $data->{src_suffix} = '.gzt';
    $data->{dst_suffix} = '.bin';

    my $self = bless {}, $class;

    $self->_init($data);

    return $self;
}

sub _make_command {
    my ($self, $params) = @_;
    return undef unless ($params->{bin_path});

    my @args = ( $params->{bin_path}.'/gztcompiler',
                 '--force',
                 '-I', $params->{bin_path}.'/proto/',
                 $self->{src},
                 '2>&1' );

    $self->{command} = join(' ', @args);
}

sub analyze_errors {
    my $self = shift;

    return $self->{result} unless ($self->{result} =~ /^Done\./m);
    return undef;
}

1;
