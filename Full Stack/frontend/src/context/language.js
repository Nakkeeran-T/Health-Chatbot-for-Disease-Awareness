import { createContext, useContext } from 'react';

export const LanguageContext = createContext(null);

/**
 * Supported language codes and their display labels.
 * To add a new language:
 *  1. Create  src/locales/<code>.json  with all translation keys.
 *  2. Add an entry here.
 * No other code changes needed.
 */
export const SUPPORTED_LANGUAGES = {
  en: { label: 'EN', nativeLabel: 'English' },
  hi: { label: 'हि', nativeLabel: 'हिंदी' },
  or: { label: 'ଓ',  nativeLabel: 'ଓଡ଼ିଆ' },
};

/**
 * Vite's import.meta.glob eagerly loads all JSON files from src/locales/*.json
 * at build time as static modules — making them available for instant lookup
 * without any runtime HTTP fetch.
 *
 * The key format is:  '../locales/en.json'  (relative to this file)
 */
const localeModules = import.meta.glob('../locales/*.json', { eager: true });

/**
 * Build a lookup map:  { en: {...}, hi: {...}, or: {...} }
 * from the eagerly-imported locale modules.
 */
const locales = Object.fromEntries(
  Object.entries(localeModules).map(([path, module]) => {
    // Extract language code from path: '../locales/en.json' → 'en'
    const code = path.replace('../locales/', '').replace('.json', '');
    return [code, module.default ?? module];
  })
);

/**
 * Falls back to English if the key is missing in the selected language.
 * Returns the key itself if not found in any locale.
 */
export function translate(lang, key, vars = {}) {
  const dict = locales[lang] ?? locales['en'] ?? {};
  const fallback = locales['en'] ?? {};
  let text = dict[key] ?? fallback[key] ?? key;

  // Simple variable interpolation: replace {varName} placeholders
  if (vars && typeof text === 'string') {
    Object.entries(vars).forEach(([k, v]) => {
      text = text.replaceAll(`{${k}}`, v ?? '');
    });
  }
  return text;
}

export const useLanguage = () => {
  const ctx = useContext(LanguageContext);
  if (!ctx) throw new Error('useLanguage must be used inside LanguageProvider');
  return ctx;
};
