try {
(
function(writer, context, http) {
    var request = context.request();
    context.State.setString("X-Yandex-ClientType", request["clientType"]);
    context.State.setString("X-Yandex-ClientVersion", request["clientVersion"]);
    var res = context.call(request["wmi-method"], request);
    context.write(res);
}
)(User.Writer, User.WmiInstance.Context, User.WmiInstance.Context.Http)
} catch (e) {
    User.WmiInstance.Context.write(JSON.stringify(e));
}
