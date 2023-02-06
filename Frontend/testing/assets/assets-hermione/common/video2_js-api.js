(function() {
    var Player = function(sandbox) {
        this._sandbox = sandbox;
    };

    /**
     * Триггерит событие плеера
     *
     * @param {String} event
     * @param {*[]} [args]
     */
    Player.prototype.trigger = function(event, args) {
        if (!this._handlers || !this._handlers[event]) {
            return;
        }

        var handlers = this._handlers[event];
        var handler;

        args = args || [];

        for (var i = 0; i < handlers.length; i++) {
            handler = handlers[i];
            handler.cb.apply(this, args);
        }
    };

    Player.prototype.update = function(uuid, options) {
        var self = this;
        this.__uuid = uuid;

        return {
            then: function(callback) {
                callback(self._sandbox);

                return {
                    catch: function() {}
                };
            }
        };
    };

    Player.prototype.stop = function() {
        this._sandbox.stop();
    };

    /**
     * Подписывает обработчик на событие
     */
    Player.prototype.on = function(event, callback) {
        var self = this;
        if (typeof event === 'object') {
            Object.keys(event).forEach(function(eventName) {
                self.on(eventName, event[eventName]);
            });
        }

        this._handlers = this._handlers || {};
        this._handlers[event] = this._handlers[event] || [];

        this._handlers[event].push({
            cb: callback
        });
    };

    var Sandbox = function() {
        this.PLAYER_STATES = {
            STOP: 0,
            PLAY: 1,
            PAUSE: 2
        };
    };

    Sandbox.prototype.init = function(player) {
        this.player = player;
    };

    Sandbox.prototype.play = function() {};

    Sandbox.prototype.pause = function() {
        this.player.trigger('onStateChange', [this.PLAYER_STATES.PAUSE]);
    };

    Sandbox.prototype.stop = function() {
        this.player.trigger('onStateChange', [this.PLAYER_STATES.STOP]);
    };

    Sandbox.prototype.seek = function() {
        this.player.trigger('onSeek');
    };

    Sandbox.prototype.volumeChange = function() {
        this.player.trigger('onVolumeChange');
    };

    Sandbox.prototype.setFullscreen = function(isFullScreen) {
        this.player.trigger('onFullScreenChange', [isFullScreen]);
    };

    BEM.DOM.decl({ block: 'video2', modName: 'js-api-mocks', modVal: true }, {
        _onStartVHPlayer: function() {
            var sandbox = new Sandbox();
            this.player = new Player(sandbox);
            sandbox.init(this.player);

            this.bindPlayerEvents(this.player);
            this.afterStart(sandbox);
        },

        bindPlayerEvents: function(player) {
            this.__base.apply(this, arguments);
        },

        afterStart: function(sandbox) {
            this._sandbox = sandbox;
            this.setMod(this._playerElem, 'visible', true);
        },

        _getCurrentPlayer: function() {
            return this.player;
        },

        _isSameContent: function() {
            return false;
        },

        getCurrentUuid: function() {
            return this.player.__uuid;
        },

        stop: function() {
            this.player.stop();
        }
    });
})();
