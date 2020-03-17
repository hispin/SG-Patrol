package com.sensoguard.detectsensor.classes;

import com.sensoguard.detectsensor.R;

import java.util.ArrayList;

import im.delight.android.languages.LanguageList;


public class LanguageManager {
    public static ArrayList<GeneralItemMenu> languagesItems;

    //Configuration the list of languages
    public static void setLanguageList() {

        languagesItems = new ArrayList<>();
        languagesItems.add(new GeneralItemMenu("English", R.drawable.ic_english_small, R.drawable.ic_english_large));
        languagesItems.add(new GeneralItemMenu("Hebrew", R.drawable.ic_hebrew_small, R.drawable.ic_hebrew_large));
        languagesItems.add(new GeneralItemMenu("French", R.drawable.ic_french_small, R.drawable.ic_french_large));
        languagesItems.add(new GeneralItemMenu("Italian", R.drawable.ic_italian_small, R.drawable.ic_italian_large));
        languagesItems.add(new GeneralItemMenu("Spanish", R.drawable.ic_spanish_small, R.drawable.ic_spanish_large));


        for (int i = 0; i < LanguageList.getHumanReadable().length; i++) {
            for (GeneralItemMenu langItem : languagesItems) {
                if (LanguageList.getHumanReadable()[i].startsWith(langItem.getKey())) {
                    langItem.setTitle(LanguageList.getHumanReadable()[i]);
                    langItem.setValue(LanguageList.getMachineReadable()[i]);
                }
            }
        }
    }

    //check if the app support this language
    public static boolean isExistLang(String value) {
        for (GeneralItemMenu langItem : languagesItems) {
            if (langItem.getValue().equals(value)) {
                return true;
            }
        }
        return false;
    }

    //get current language
    public static GeneralItemMenu getCurrentLang(String value) {
        if (languagesItems == null) {
            return null;
        }
        for (GeneralItemMenu langItem : languagesItems) {
            if (langItem.getValue().equals(value)) {
                return langItem;
            }
        }
        return null;
    }

}
