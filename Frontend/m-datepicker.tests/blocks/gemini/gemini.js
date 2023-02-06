// Поскольку today_yes выставляется дню в зависимости от текущей даты,
// необходимо подставить свою для скриншота
(function() {
    var D = Date;

    Date = function() {
        return new D(2012, 9, 11);
    };
    Date.prototype = D.prototype;
    Date.__proto__ = D;
})();
