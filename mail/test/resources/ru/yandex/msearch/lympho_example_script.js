var MrDocCollector = Java.type('ru.yandex.lympho.LymphoSearchCollector');

var output = context.httpOut("http://localhost:RCV_PORT/sendBatch", 1);
var SearchCallback = Java.extend(MrDocCollector, {
    document: function(doc, fields) {
        output.startObject();
        for each (var field in fields) {
            output.key(field);
            output.value(doc.getString(field));
        }
        output.endObject();
    }
});

var SearchCallback2 = Java.extend(MrDocCollector, {
    document: function(doc, fields) {
        for each (var field in fields) {
            context.search("1", "text:" + doc.getString(field), "keyword", 10, new SearchCallback());
        }
    }
});


//print(context);
context.search("1", "boolean:true", "keyword", 10, new SearchCallback2());