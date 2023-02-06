$syndict = $ARGV[0];
print STDERR "read $syndict\n"; 
open F, "<$syndict";

$disamb_lemmas_count=0;
while (<F>) {
	chomp;
	($q, $qw, $l) = split(/\t/);
	$good{"$q\t$qw"} = $l;
	$disamb_lemmas_count++;
}

$plus =0;
$minus=0;
while (<STDIN>) {
	chomp();
	if ($_ =~ /^src: \[\x1A?(.*)\]$/) {
		$src = $1;
	}
	if ($_ =~ /^Disamb.Disamb[0-9]*: \[(.+)->(.+)\]$/) {
		$word = $1;	
		$l_m = $2;	
		$l_h = $good{"$src\t$word"};
		$used{"$src\t$word"} = 1;
		if (($l_h eq "Õ»Œƒ»Õ") || ($l_h eq "Õ≈«Õ¿ﬁ") || ($l_h eq "")) {
			print "undef $src\t$word\t$l_m\n";
			next;
		}
		@ll = split (/\|/, $l_m);
		$lemmas_count += scalar @ll;
		if (grep /^$l_h$/, @ll) {
			$plus++;
		}
		else {
			$minus++;
			print "minus $src\t$word\t$l_h != $l_m\n";
		}
	}
}


for $c (keys %good) {
	if (! defined $used{$c} ) {
		print "not found $c\n";		
	}
}

$prec = $plus/($plus+$minus);
$recall = $plus/$disamb_lemmas_count;
$avg_amb = ($plus+$minus)/$lemmas_count;
print STDERR "minus=$minus,plus=$plus; prec=$prec; recall=$recall;avg_amb=$avg_amb\n";
