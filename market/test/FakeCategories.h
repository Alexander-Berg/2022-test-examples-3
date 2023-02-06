#pragma once
#include <market/report/library/constants/constants.h>
#include <market/library/map_ops_bicycle/map_ops_bicycle.h>

class FakeCategories {
public:
    void add(unsigned parent_id, unsigned child_id) {
        child_parent_[child_id] = parent_id;
    }
    unsigned getParent(unsigned id) const {
        return get(child_parent_, id, 0);
    }
    int getLevel(unsigned id) const {
        if (id != 0)
            return 1 + getLevel(getParent(id));
        else
            return -1;
    }
    bool hasAncestor(unsigned id, unsigned root) const {
        if (id != 0)
            return id == root || hasAncestor(getParent(id), root);
        else
            return false;
    }

private:
    using ChildParentMap = std::map<unsigned, unsigned>;
    ChildParentMap child_parent_;
};

const unsigned ROOT_CATEGORY = 90401;
const unsigned CLOTHES_CATEGORY = 7877999;
const unsigned PANTS_CATEGORY = 7811903;
const unsigned WARDROBE_CATEGORY = 90675;
const unsigned MEN_PANTS_CATEGORY = 7812152;
const unsigned MEN_TSHIRT_CATEGORY = 7812151;
const unsigned MATTRESS_CATEGORY = 1003092;
const unsigned WOMEN_CLOTHES_CATEGORY = 23945908;
const unsigned CHILD_GOODS_CATEGORY = 90764;
const unsigned TOYS_CATEGORY = 90783;
const unsigned RATTLES_AND_TEETHERS_CATEGORY = 10682496;
const unsigned MOBILES_CATEGORY = 91491;
const unsigned WOMEN_SPORT_CLOTHES_CATEGORY = 8268516;

inline const FakeCategories& getFakeCategories() {
    static THolder<FakeCategories> cats;
    if (!cats.Get()) {
        cats.Reset(new FakeCategories());
        cats->add(ROOT_CATEGORY, CLOTHES_CATEGORY);
        cats->add(CLOTHES_CATEGORY, WOMEN_CLOTHES_CATEGORY);
        cats->add(WOMEN_CLOTHES_CATEGORY, PANTS_CATEGORY);
        cats->add(WOMEN_CLOTHES_CATEGORY, WOMEN_SPORT_CLOTHES_CATEGORY);
        cats->add(CLOTHES_CATEGORY, MEN_PANTS_CATEGORY);
        cats->add(CLOTHES_CATEGORY, MEN_TSHIRT_CATEGORY);
        cats->add(ROOT_CATEGORY, MATTRESS_CATEGORY);
        cats->add(ROOT_CATEGORY, WARDROBE_CATEGORY);
        cats->add(ROOT_CATEGORY, CHILD_GOODS_CATEGORY);
        cats->add(ROOT_CATEGORY, MOBILES_CATEGORY);
        cats->add(CHILD_GOODS_CATEGORY, TOYS_CATEGORY);
        cats->add(CHILD_GOODS_CATEGORY, RATTLES_AND_TEETHERS_CATEGORY);
    }
    return *cats;
}
