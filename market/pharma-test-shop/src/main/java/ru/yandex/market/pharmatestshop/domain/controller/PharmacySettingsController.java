package ru.yandex.market.pharmatestshop.domain.controller;


import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import ru.yandex.market.pharmatestshop.domain.pharmacy.Pharmacy;
import ru.yandex.market.pharmatestshop.domain.pharmacy.PharmacyErrors;
import ru.yandex.market.pharmatestshop.domain.pharmacy.PharmacyService;

//Настройки магазина
@Controller
@RequestMapping(
        value = {"/pharmacies"},
        produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class PharmacySettingsController {

    final
    PharmacyService pharmacyService;

    @Autowired
    public PharmacySettingsController(PharmacyService pharmacyService) {
        this.pharmacyService = pharmacyService;
    }

    /**
     * @return html page with table of all pharmacies in db
     */
    @GetMapping()
    public String getAllPharmacies(Model model) {
        model.addAttribute("pharmacies", pharmacyService.getAllPharmacies());
        return "all_pharmacies";
    }

    /**
     * @return html page with information about one pharmacy with id
     */
    @GetMapping("/{shop_id}")
    public String getOnePharmacy(@PathVariable("shop_id") long shop_id, Model model) {
        model.addAttribute("pharmacy", pharmacyService.getPharmacy(shop_id));
        return "one_pharmacy";
    }

    /**
     * @return html page with form for creating new pharmacy
     */
    @GetMapping("/new")
    public String newPharmacy(@ModelAttribute("pharmacy") Pharmacy pharmacy) {
        return "new";
    }

    /**
     * Create new pharmacy and returns to page with all pharmacies
     * or write all errors
     */
    @PostMapping()
    public String create(@ModelAttribute("pharmacy") @Valid Pharmacy pharmacy,
                         BindingResult bindingResult, @ModelAttribute("pharmacy_error") PharmacyErrors error) {
        if (bindingResult.hasErrors() || error.hasErrors(pharmacy)) {

            return "new";
        }

        pharmacyService.saveOrUpdate(pharmacy);
        return "redirect:/pharmacies";
    }

    /**
     * @return html page for editing pharmacy with shop_id
     */
    @GetMapping("/{shop_id}/edit")
    public String edit(Model model, @PathVariable("shop_id") long shop_id) {
        model.addAttribute("pharmacy", pharmacyService.getPharmacy(shop_id));
        return "edit";
    }

    /**
     * Update pharmacy (with id=shop_id) and returns to page with all pharmacies
     * or write all errors
     */
    @PostMapping("/{shop_id}")
    public String update(@ModelAttribute("pharmacy") @Valid Pharmacy pharmacy, BindingResult bindingResult,
                         @PathVariable("shop_id") long shop_id,
                         @ModelAttribute("pharmacy_error") PharmacyErrors error) {
        pharmacy.setShopId(shop_id);

        if (bindingResult.hasErrors() || error.hasErrors(pharmacy)) {
            return "edit";
        }


        pharmacyService.saveOrUpdate(pharmacy);
        return "redirect:/pharmacies";
    }

    /**
     * Delete pharmacy from db with id=shop_id
     *
     * @return html page with all pharmacies
     */
    @PostMapping("/delete/{shop_id}")
    public String delete(@PathVariable("shop_id") long shop_id) {
        pharmacyService.delete(shop_id);
        return "redirect:/pharmacies";
    }

}
