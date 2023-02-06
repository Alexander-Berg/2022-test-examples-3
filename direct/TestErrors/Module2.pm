package TestErrors::Module2;
    
use Direct::Errors;

error ReqField => (code => 10, text => '');
error BadLang => (code => 101, text => '');
warning BadLang => (code => 101, text => '');
error MaxLength => (code => 10, text => '');

1;