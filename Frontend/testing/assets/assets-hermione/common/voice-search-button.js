(function() {
    var eventData = {
        session: {
            streamer: {
                recorder: {
                    getAnalyserNode: function() {
                        return {
                            frequencyBinCount: 4,
                            getByteFrequencyData: function(uArray) {
                                for (var i = 0; i < uArray.length; i++) {
                                    uArray[i] = 255;
                                }
                            }
                        };
                    }
                },
                abort: function() {}
            }
        }
    };

    function replaceMethods() {
        BEM.decl('i-voice', {}, {
            startRecognize: function() {
                this.trigger(this.events.BEFORE_START_RECOGNIZE, eventData);
                this.trigger(this.events.START_RECOGNIZE, eventData);
                this.trigger(this.events.SPEECH_KIT_ACCESS_GRANTED, eventData);
                this.trigger(this.events.SPEECH_KIT_INIT, eventData);
            },

            stopRecognize: function() {
                this.trigger(this.events.STOP_RECOGNIZE, eventData);
            }
        });

        BEM.DOM.decl('voice-search-popup', {
            _calcSoundAmplitudePerFrame: function() {
                this._onSoundChangeAmplitude(1);
            }
        });
    }

    BEM.DOM.decl('voice-search-button', {
        _checkMicSupport: function() {},
        _onSuccessLoad: function() {
            // костыли, для правильного переопределения после ajax загрузки
            replaceMethods();

            this.__base.apply(this, arguments);
        }
    });
})();
