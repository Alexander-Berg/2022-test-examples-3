module.exports = function() {
    return this
        .execute(function() {
            // хачим position:sticky
            // чтобы не дать таким элементам возможности подкливаться к сриншоту несолько раз при сколле
            document.getElementById('root').style.overflow = 'hidden';
        });
};
