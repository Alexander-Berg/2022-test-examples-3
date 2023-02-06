package TestErrors::Module3;
    
use Direct::Errors;

error ReqField => (code => 10, text => '');
error BadLang => (code => 101, text => '');
warning BadLang => (code => 101, text => '');
error MaxLength => (code => 18, text => '');
error InvalidChars => (code => 78, text => '');

sub error_MaxLength {}

1;