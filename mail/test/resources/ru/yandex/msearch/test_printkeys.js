context.keys({'field': 'keyword', 'print-freqs': 'true'}).forEach(function (k) { print(k.term() + " " + k.freq()) });

//print(context);
//context.search("1", "boolean:true", "keyword", 10, new SearchCallback2());