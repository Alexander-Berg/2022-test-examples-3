package util

type CategoriesReader func(file string) map[int]bool

func (categoriesReader CategoriesReader) ReadCashbackCategories() map[int]bool {
	return categoriesReader("./cb_categories.json")
}

func (categoriesReader CategoriesReader) ReadNonCashbackCategories() map[int]bool {
	return categoriesReader("./noncb_categories.json")
}

func (categoriesReader CategoriesReader) ReadCategories() map[int]bool {
	result := categoriesReader.ReadCashbackCategories()
	noncbCategories := categoriesReader.ReadNonCashbackCategories()
	for k := range noncbCategories {
		result[k] = true
	}
	return result
}
