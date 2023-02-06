BEGIN {
	mg=0;
	un=0;
	hl=0;
	sy=0;
    pr=0;
    sl=0;
} 
/logging mongos/ {
	mgs[mg++]=$NF; 
} 
/logging disk-unit/ {
	unt[un++]=$NF; 
} 
/logging system\./ {
	sys[sy++]=$NF; 
} 
/logging hardlinks\./ {
	hrd[hl++]=$NF; 
} 
/read=SLAVE/ {
    sl+=1;
}
/read=PRIMARY/ {
    pr+=1;
}
END {
	printf "mongos_timings ";
	for (x in mgs) {
		printf "%.3f ", mgs[x]
	}
	printf "\nunit_timings ";
	for (x in unt) {
		printf "%.3f ", unt[x]
	}
	printf "\nsystem_timings ";
	for (x in sys) {
		printf "%.3f ", sys[x]
	}
	printf "\nhardlink_timings ";
	for (x in hrd) {
		printf "%.3f ", hrd[x]
	}
	printf "\nmongos_count %d\nunit_count %d\nsystem_count %d\nhardlink_count %d\nslave_count %s\nprimary_count %s\n", mg,un,sy,hl,sl,pr
}
