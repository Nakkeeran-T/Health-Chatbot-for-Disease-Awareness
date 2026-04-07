import { useState, useEffect } from 'react';
import { diseaseAPI } from '../services/api';
import { useLanguage } from '../context/language';
import './InfoPage.css';

const categoryColors = {
  'Vector-borne': '#00d4aa',
  'Airborne':     '#ff6b6b',
  'Waterborne':   '#6c63ff',
  'Foodborne':    '#ffd93d',
  'Respiratory':  '#ff8c00',
};

const DiseasesPage = () => {
  const [diseases, setDiseases] = useState([]);
  const [loading, setLoading]   = useState(true);
  const [search, setSearch]     = useState('');
  const [selected, setSelected] = useState(null);
  const { t, language } = useLanguage();

  useEffect(() => {
    diseaseAPI.getAll().then(res => setDiseases(res.data)).finally(() => setLoading(false));
  }, []);

  const getLocalizedName = (d) => {
    if (language === 'hi' && d.nameHi) return d.nameHi;
    if (language === 'or' && d.nameOr) return d.nameOr;
    return d.name;
  };

  const getSymptoms = (d) => {
    if (language === 'hi' && d.symptomsHi) return d.symptomsHi;
    if (language === 'or' && d.symptomsOr) return d.symptomsOr;
    return d.symptoms;
  };

  const getPrevention = (d) => {
    if (language === 'hi' && d.preventionHi) return d.preventionHi;
    if (language === 'or' && d.preventionOr) return d.preventionOr;
    return d.prevention;
  };

  const filtered = diseases.filter(d =>
    d.name.toLowerCase().includes(search.toLowerCase()) ||
    d.category?.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <div className="page info-page">
      <div className="container">
        <div className="page-header">
          <h1>🦠 {t('diseasesTitle')}</h1>
          <p>{t('diseasesSub')}</p>
        </div>

        <input
          className="input-field search-input"
          placeholder={t('searchPlaceholder')}
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />

        {loading ? (
          <div className="loading-grid">
            {[...Array(6)].map((_, i) => <div key={i} className="skeleton-card"></div>)}
          </div>
        ) : filtered.length === 0 ? (
          <div className="empty-state">
            <div className="empty-icon">🔍</div>
            <h3>{t('noData')}</h3>
          </div>
        ) : (
          <div className="cards-grid">
            {filtered.map((d) => (
              <div key={d.id} className="info-card" onClick={() => setSelected(selected?.id === d.id ? null : d)}>
                <div className="info-card-header">
                  <div>
                    <h3 className="info-card-name">{getLocalizedName(d)}</h3>
                    {d.name !== getLocalizedName(d) && <p className="info-card-orig">{d.name}</p>}
                  </div>
                  <div className="info-card-badges">
                    <span className="badge" style={{ background: `${categoryColors[d.category]}20`, color: categoryColors[d.category], borderColor: `${categoryColors[d.category]}40` }}>
                      {d.category}
                    </span>
                    {d.contagious && <span className="badge badge-danger">{t('contagious')}</span>}
                  </div>
                </div>

                <p className="info-card-desc">{d.description?.substring(0, 120)}...</p>

                {selected?.id === d.id && (
                  <div className="info-card-expanded fade-in">
                    <div className="info-section">
                      <h4>🤒 {t('symptoms')}</h4>
                      <p>{getSymptoms(d)}</p>
                    </div>
                    <div className="info-section">
                      <h4>🛡️ {t('prevention')}</h4>
                      <p>{getPrevention(d)}</p>
                    </div>
                    <div className="info-section">
                      <h4>💊 {t('treatment')}</h4>
                      <p>{d.treatment}</p>
                    </div>
                    {d.icdCode && (
                      <div className="icd-code">{t('icdCode')}: {d.icdCode}</div>
                    )}
                  </div>
                )}

                <button className="expand-btn">
                  {selected?.id === d.id ? `▲ ${t('showLess')}` : `▼ ${t('readMore')}`}
                </button>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

export default DiseasesPage;
