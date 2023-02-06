local module = {
    MILLISECOND = 1,
};

module.SECOND = module.MILLISECOND * 1000;
module.MINUTE = module.SECOND * 60;
module.HOUR = module.MINUTE * 60;
module.DAY = module.HOUR * 24;
module.WEEK = module.DAY * 7;
module.MONTH = module.WEEK * 4;

return module;