var Collector = Java.type('ru.yandex.lympho.LymphoSearchCollector');

var output = context.httpOut("http://localhost:RCV_PORT/sendBatch", 2);

var SearchCallback = Java.extend(Collector, {
    document: function(doc, fields) {
        output.startObject();
        for each (var field in fields) {
            output.key(field);
            output.value(doc.getString(field));
        }
        output.endObject();
    }
});

//print(context);
context.search("prefix=1&text=boolean:true&get=keyword&limit=10", new SearchCallback());
