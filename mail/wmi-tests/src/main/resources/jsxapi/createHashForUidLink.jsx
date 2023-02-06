try {
(
function(writer, context, http) {
    var uid = 'uid'
    var url = 'url'
    var hash = context.Wmi.createHashForUidLink(uid, url)
    var valid = context.Wmi.validateHashForUidLink(uid, url, hash)
    writer.write(valid)
}
)(User.Writer, User.WmiInstance.Context, User.WmiInstance.Context.Http)
} catch (e) {
    User.WmiInstance.Context.write(JSON.stringify(e));
}