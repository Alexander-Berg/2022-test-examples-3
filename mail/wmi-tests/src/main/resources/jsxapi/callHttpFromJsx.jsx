try {
(
function(writer, context, http) {
    context.jcall("account_information", {});
    var handle = context.Http.create("http://settings.mail.yandex.net/get_all_profile",
    { proxy: false, type: "get", timeout: 10000 },
    { }, { }  );
    context.write(handle.status());
}
)(User.Writer, User.WmiInstance.Context, User.WmiInstance.Context.Http)
} catch (e) {
    User.WmiInstance.Context.write(JSON.stringify(e));
}