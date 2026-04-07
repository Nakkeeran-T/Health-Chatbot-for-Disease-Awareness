import { useState } from 'react';
import { LanguageContext, translate, SUPPORTED_LANGUAGES } from './language';

export const LanguageProvider = ({ children }) => {
  const [language, setLanguage] = useState(
    localStorage.getItem('language') || 'en'
  );

  const changeLanguage = (lang) => {
    if (!SUPPORTED_LANGUAGES[lang]) return; // guard against unknown codes
    setLanguage(lang);
    localStorage.setItem('language', lang);
  };

  /**
   * t(key)               → plain translation
   * t(key, { name: 'X' }) → translation with variable interpolation
   */
  const t = (key, vars) => translate(language, key, vars);

  return (
    <LanguageContext.Provider value={{ language, changeLanguage, t, SUPPORTED_LANGUAGES }}>
      {children}
    </LanguageContext.Provider>
  );
};
